"""
业务逻辑层(Service)
---------------------------------------------------------------
负责借用记录的业务规则:
  * 参数校验
  * 借出时扣减物资库存(事务)
  * 归还时回补物资库存(事务)
  * 逾期状态自动识别
"""

import logging
import sys
from pathlib import Path
from datetime import datetime

# 兼容直接运行脚本的情况
current_dir = Path(__file__).resolve().parent.parent
if str(current_dir) not in sys.path:
    sys.path.insert(0, str(current_dir))

from app.repository import STATUS_BORROWED, STATUS_RETURNED, STATUS_OVERDUE, VALID_STATUS

logger = logging.getLogger(__name__)


class BorrowService:
    """借用记录业务服务"""

    def __init__(self, repository):
        self.repo = repository

    # --------------------------------------------------------------
    # 参数校验
    # --------------------------------------------------------------
    @staticmethod
    def validate_payload(payload, for_create=True):
        """校验请求体,返回错误信息(None 表示校验通过)"""
        if for_create:
            for field in ("personnelId", "materialId"):
                if payload.get(field) in (None, ""):
                    return "%s 为必填项" % field
            try:
                int(payload["personnelId"])
                int(payload["materialId"])
            except (TypeError, ValueError):
                return "personnelId 与 materialId 必须为整数"

        qty = payload.get("quantity")
        if qty is not None:
            try:
                if int(qty) <= 0:
                    return "quantity 必须为正整数"
            except (TypeError, ValueError):
                return "quantity 必须为正整数"

        status = payload.get("status")
        if status is not None and status not in VALID_STATUS:
            return "status 必须是 %s 之一" % (VALID_STATUS,)

        return None

    # --------------------------------------------------------------
    # 查询
    # --------------------------------------------------------------
    def list_borrows(self, filters):
        return self.repo.list_borrows(filters)

    def get_borrow(self, borrow_id):
        return self.repo.find_by_id(borrow_id)

    def statistics(self):
        return self.repo.statistics()

    # --------------------------------------------------------------
    # 新建(借出):扣减库存,事务保证一致性
    # --------------------------------------------------------------
    def create_borrow(self, payload):
        material_id = int(payload["materialId"])
        quantity = int(payload.get("quantity") or 1)

        with self.repo.connection() as conn:
            try:
                with conn.cursor() as cur:
                    cur.execute("SELECT quantity FROM material WHERE id = %s", [material_id])
                    row = cur.fetchone()
                    if row is None:
                        conn.rollback()
                        raise ValueError("物资 ID=%d 不存在,无法借用" % material_id)
                    stock = int(row["quantity"])
                    if stock < quantity:
                        conn.rollback()
                        raise ValueError(
                            "物资 ID=%d 库存不足: 现有 %d,请求 %d" % (material_id, stock, quantity)
                        )

                # 扣减库存
                self.repo.reduce_material_quantity(material_id, quantity, conn)

                # 写入借用记录(默认借用中)
                data = {
                    "personnelId": int(payload["personnelId"]),
                    "materialId": material_id,
                    "quantity": quantity,
                    "borrowDate": payload.get("borrowDate"),
                    "expectReturn": payload.get("expectReturn"),
                    "status": payload.get("status") or STATUS_BORROWED,
                    "purpose": payload.get("purpose"),
                    "remark": payload.get("remark"),
                }
                new_id = self.repo.create(data, conn=conn)
                conn.commit()
            except Exception:
                conn.rollback()
                raise

        return self.repo.find_by_id(new_id)

    # --------------------------------------------------------------
    # 更新(归还):回补库存,事务保证一致性
    # --------------------------------------------------------------
    def update_borrow(self, borrow_id, payload):
        old = self.repo.find_raw_by_id(borrow_id)
        if old is None:
            return None

        old_status = old["status"]
        old_qty = int(old["quantity"])
        new_status = payload.get("status", old_status)

        # 状态流转:借用中 -> 已归还,需要回补库存
        should_restore = (old_status == STATUS_BORROWED and new_status == STATUS_RETURNED)

        # 归还时若未显式给出实际归还时间,则自动填当前时间
        data = dict(payload)
        if new_status == STATUS_RETURNED and not data.get("actualReturn"):
            data["actualReturn"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        with self.repo.connection() as conn:
            try:
                if should_restore:
                    self.repo.increase_material_quantity(old["material_id"], old_qty, conn)
                self.repo.update(borrow_id, data, conn=conn)
                conn.commit()
            except Exception:
                conn.rollback()
                raise

        return self.repo.find_by_id(borrow_id)

    # --------------------------------------------------------------
    # 删除:若仍在借用中,回补库存
    # --------------------------------------------------------------
    def delete_borrow(self, borrow_id):
        old = self.repo.find_raw_by_id(borrow_id)
        if old is None:
            return False

        with self.repo.connection() as conn:
            try:
                if old["status"] == STATUS_BORROWED:
                    self.repo.increase_material_quantity(
                        old["material_id"], int(old["quantity"]), conn
                    )
                self.repo.delete(borrow_id, conn=conn)
                conn.commit()
            except Exception:
                conn.rollback()
                raise
        return True
