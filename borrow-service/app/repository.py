"""
数据库访问层(Repository)
---------------------------------------------------------------
借用记录服务通过 PyMySQL 访问 MySQL 的 borrow 表。
使用 dbutils 连接池(DBUtils.PooledDB)管理连接。
"""

import logging
import pymysql
from pymysql.cursors import DictCursor
from contextlib import contextmanager

try:
    from dbutils.pooled_db import PooledDB
except ImportError:
    PooledDB = None

logger = logging.getLogger(__name__)

# 借出状态枚举
STATUS_BORROWED = "BORROWED"
STATUS_RETURNED = "RETURNED"
STATUS_OVERDUE = "OVERDUE"
STATUS_LOST = "LOST"
VALID_STATUS = (STATUS_BORROWED, STATUS_RETURNED, STATUS_OVERDUE, STATUS_LOST)


class BorrowRepository:
    """borrow 表的数据访问对象"""

    def __init__(self, settings):
        self._settings = settings
        self._pool = None

    # --------------------------------------------------------------
    # 连接池
    # --------------------------------------------------------------
    @property
    def pool(self):
        if self._pool is None:
            if PooledDB is not None:
                # 使用 DBUtils 连接池
                self._pool = PooledDB(
                    creator=pymysql,
                    maxconnections=self._settings.DB_POOL_SIZE,
                    mincached=1,
                    maxcached=5,
                    host=self._settings.DB_HOST,
                    port=self._settings.DB_PORT,
                    user=self._settings.DB_USER,
                    password=self._settings.DB_PASSWORD,
                    database=self._settings.DB_NAME,
                    charset=self._settings.DB_CHARSET,
                    cursorclass=DictCursor,
                    autocommit=False,
                    connect_timeout=10,
                    read_timeout=30,
                    write_timeout=30,
                )
            else:
                # 降级:每次新建连接(开发环境)
                self._pool = _SimpleConnectionPool(self._settings)
        return self._pool

    @contextmanager
    def connection(self):
        """从池中借出一个连接,退出时自动归还"""
        if hasattr(self.pool, 'dedicated_connection'):
            conn = self.pool.dedicated_connection()
        elif hasattr(self.pool, 'connection'):
            conn = self.pool.connection()
        else:
            conn = self.pool.connection()
        try:
            yield conn
        finally:
            conn.close()

    @contextmanager
    def cursor(self):
        """借连接 + 开游标"""
        with self.connection() as conn:
            with conn.cursor() as cur:
                yield conn, cur

    def ping(self):
        """连通性检查"""
        with self.cursor() as (_conn, cur):
            cur.execute("SELECT 1")
            return cur.fetchone()

    # --------------------------------------------------------------
    # 查询
    # --------------------------------------------------------------
    def _build_filters(self, filters):
        clauses = []
        params = []

        if filters.get("personnelId"):
            clauses.append("b.personnel_id = %s")
            params.append(filters["personnelId"])
        if filters.get("materialId"):
            clauses.append("b.material_id = %s")
            params.append(filters["materialId"])
        if filters.get("status"):
            clauses.append("b.status = %s")
            params.append(filters["status"])
        if filters.get("keyword"):
            clauses.append("(b.purpose LIKE %s OR b.remark LIKE %s)")
            kw = "%" + filters["keyword"] + "%"
            params.extend([kw, kw])

        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        return where, params

    def list_borrows(self, filters):
        """分页查询借用记录,联表带出借用人姓名与物资名称"""
        where, params = self._build_filters(filters)

        page = max(1, int(filters.get("page") or 1))
        page_size = min(200, max(1, int(filters.get("pageSize") or 10)))
        offset = (page - 1) * page_size

        base_sql = (
            "FROM borrow b "
            "LEFT JOIN personnel p ON b.personnel_id = p.id "
            "LEFT JOIN material  m ON b.material_id  = m.id "
        )

        with self.cursor() as (_conn, cur):
            cur.execute("SELECT COUNT(*) AS total " + base_sql + where, params)
            total = int(cur.fetchone()["total"])

            cur.execute(
                "SELECT b.*, p.name AS personnel_name, m.name AS material_name, "
                "m.unit AS material_unit " + base_sql + where +
                " ORDER BY b.id DESC LIMIT %s OFFSET %s",
                params + [page_size, offset],
            )
            items = cur.fetchall()

        return {
            "items": items,
            "total": total,
            "page": page,
            "pageSize": page_size,
            "totalPages": (total + page_size - 1) // page_size,
        }

    def find_by_id(self, borrow_id):
        """按ID查询单条记录(联表)"""
        sql = (
            "SELECT b.*, p.name AS personnel_name, m.name AS material_name, "
            "m.unit AS material_unit "
            "FROM borrow b "
            "LEFT JOIN personnel p ON b.personnel_id = p.id "
            "LEFT JOIN material  m ON b.material_id  = m.id "
            "WHERE b.id = %s"
        )
        with self.cursor() as (_conn, cur):
            cur.execute(sql, [borrow_id])
            return cur.fetchone()

    def find_raw_by_id(self, borrow_id):
        """按ID查询原始记录(不联表)"""
        with self.cursor() as (_conn, cur):
            cur.execute("SELECT * FROM borrow WHERE id = %s", [borrow_id])
            return cur.fetchone()

    def get_material_quantity(self, material_id, conn=None):
        """查询物资当前库存数量"""
        sql = "SELECT quantity FROM material WHERE id = %s"
        if conn is not None:
            with conn.cursor() as cur:
                cur.execute(sql, [material_id])
                row = cur.fetchone()
        else:
            with self.cursor() as (_c, cur):
                cur.execute(sql, [material_id])
                row = cur.fetchone()
        return int(row["quantity"]) if row else None

    def reduce_material_quantity(self, material_id, delta, conn):
        """扣减/回补物资库存(需在事务内)"""
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE material SET quantity = quantity - %s WHERE id = %s AND quantity >= %s",
                [delta, material_id, delta],
            )
            affected = cur.rowcount

    def increase_material_quantity(self, material_id, delta, conn):
        """回补物资库存(需在事务内)"""
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE material SET quantity = quantity + %s WHERE id = %s",
                [delta, material_id],
            )

    # --------------------------------------------------------------
    # 写入
    # --------------------------------------------------------------
    def create(self, data, conn=None):
        """新增借用记录"""
        sql = (
            "INSERT INTO borrow "
            "(personnel_id, material_id, quantity, borrow_date, expect_return, "
            " actual_return, status, purpose, remark) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"
        )
        params = [
            data["personnelId"],
            data["materialId"],
            data.get("quantity", 1),
            data.get("borrowDate"),
            data.get("expectReturn"),
            data.get("actualReturn"),
            data.get("status", STATUS_BORROWED),
            data.get("purpose"),
            data.get("remark"),
        ]
        if conn is not None:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                return cur.lastrowid
        with self.cursor() as (c, cur):
            cur.execute(sql, params)
            c.commit()
            return cur.lastrowid

    def update(self, borrow_id, data, conn=None):
        """更新借用记录(部分字段)"""
        field_map = {
            "quantity": "quantity",
            "borrowDate": "borrow_date",
            "expectReturn": "expect_return",
            "actualReturn": "actual_return",
            "status": "status",
            "purpose": "purpose",
            "remark": "remark",
        }
        sets, params = [], []
        for key, column in field_map.items():
            if key in data and data[key] is not None:
                sets.append("%s = %%s" % column)
                params.append(data[key])
        if not sets:
            return
        params.append(borrow_id)
        sql = "UPDATE borrow SET %s WHERE id = %%s" % ", ".join(sets)

        if conn is not None:
            with conn.cursor() as cur:
                cur.execute(sql, params)
        else:
            with self.cursor() as (c, cur):
                cur.execute(sql, params)
                c.commit()

    def delete(self, borrow_id, conn=None):
        """删除借用记录,返回是否删除成功"""
        sql = "DELETE FROM borrow WHERE id = %s"
        if conn is not None:
            with conn.cursor() as cur:
                cur.execute(sql, [borrow_id])
                return cur.rowcount > 0
        with self.cursor() as (c, cur):
            cur.execute(sql, [borrow_id])
            c.commit()
            return cur.rowcount > 0

    # --------------------------------------------------------------
    # 统计
    # --------------------------------------------------------------
    def statistics(self):
        """借用情况统计"""
        with self.cursor() as (_conn, cur):
            cur.execute(
                "SELECT status, COUNT(*) AS cnt, SUM(quantity) AS total_qty "
                "FROM borrow GROUP BY status"
            )
            by_status = cur.fetchall()

            cur.execute(
                "SELECT p.name AS personnel_name, COUNT(*) AS cnt "
                "FROM borrow b JOIN personnel p ON b.personnel_id = p.id "
                "GROUP BY b.personnel_id ORDER BY cnt DESC LIMIT 10"
            )
            top_personnels = cur.fetchall()

            cur.execute(
                "SELECT m.name AS material_name, COUNT(*) AS cnt "
                "FROM borrow b JOIN material m ON b.material_id = m.id "
                "GROUP BY b.material_id ORDER BY cnt DESC LIMIT 10"
            )
            top_materials = cur.fetchall()

        return {
            "byStatus": by_status,
            "topPersonnels": top_personnels,
            "topMaterials": top_materials,
        }


class _SimpleConnectionPool:
    """无 DBUtils 时的降级连接池:每次新建连接"""

    def __init__(self, settings):
        self._settings = settings

    def connection(self):
        conn = pymysql.connect(
            host=self._settings.DB_HOST,
            port=self._settings.DB_PORT,
            user=self._settings.DB_USER,
            password=self._settings.DB_PASSWORD,
            database=self._settings.DB_NAME,
            charset=self._settings.DB_CHARSET,
            cursorclass=DictCursor,
            autocommit=False,
            connect_timeout=10,
            read_timeout=30,
            write_timeout=30,
        )
        return conn
