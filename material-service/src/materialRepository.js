/**
 * 物资领域模型 / Repository
 * ---------------------------------------------------------------
 * 负责 `material` 表的全部数据访问逻辑(纯数据层,不涉及 HTTP)。
 */
'use strict';

const db = require('../src/db');

/** 允许排序的字段白名单(防止 SQL 注入) */
const SORTABLE = new Set(['id', 'name', 'category', 'quantity', 'price', 'created_at', 'expiry_date', 'status']);

/** 状态枚举 */
const STATUS = {
  IN_STOCK: 'IN_STOCK',
  LOW_STOCK: 'LOW_STOCK',
  SCRAPPED: 'SCRAPPED'
};

/**
 * 构造查询条件片段
 * @returns {{sql:string, params:Array}}
 */
function buildConditions(filters = {}) {
  const clauses = [];
  const params = [];

  if (filters.category) {
    clauses.push('category = ?');
    params.push(filters.category);
  }
  if (filters.status) {
    clauses.push('status = ?');
    params.push(filters.status);
  }
  if (filters.keyword) {
    clauses.push('(name LIKE ? OR specification LIKE ? OR supplier LIKE ?)');
    const kw = `%${filters.keyword}%`;
    params.push(kw, kw, kw);
  }
  if (filters.storageRoom) {
    clauses.push('storage_room = ?');
    params.push(filters.storageRoom);
  }
  if (filters.minQuantity !== undefined && filters.minQuantity !== '') {
    clauses.push('quantity >= ?');
    params.push(Number(filters.minQuantity));
  }
  if (filters.maxQuantity !== undefined && filters.maxQuantity !== '') {
    clauses.push('quantity <= ?');
    params.push(Number(filters.maxQuantity));
  }

  return {
    sql: clauses.length ? `WHERE ${clauses.join(' AND ')}` : '',
    params
  };
}

/**
 * 分页查询物资列表
 */
async function findAll(filters = {}) {
  const { sql: where, params } = buildConditions(filters);

  const page = Math.max(1, parseInt(filters.page || '1', 10));
  const pageSize = Math.min(200, Math.max(1, parseInt(filters.pageSize || '10', 10)));
  const offset = (page - 1) * pageSize;

  let orderBy = 'id DESC';
  if (filters.sortBy && SORTABLE.has(filters.sortBy)) {
    const dir = String(filters.order || 'desc').toLowerCase() === 'asc' ? 'ASC' : 'DESC';
    orderBy = `${filters.sortBy} ${dir}`;
  }

  const countSql = `SELECT COUNT(*) AS total FROM material ${where}`;
  const totalRow = await db.query(countSql, params);
  const total = Number(totalRow[0].total);

  const listSql = `SELECT * FROM material ${where} ORDER BY ${orderBy} LIMIT ? OFFSET ?`;
  const rows = await db.query(listSql, [...params, pageSize, offset]);

  return {
    items: rows,
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  };
}

/**
 * 根据ID查询单条物资
 */
async function findById(id) {
  const rows = await db.query('SELECT * FROM material WHERE id = ?', [id]);
  return rows.length ? rows[0] : null;
}

/**
 * 新增物资
 */
async function create(material) {
  const sql = `INSERT INTO material
      (name, category, specification, quantity, unit, price,
       storage_room, supplier, purchase_date, expiry_date, status)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`;
  const params = [
    material.name,
    material.category,
    material.specification || null,
    material.quantity != null ? material.quantity : 0,
    material.unit || '个',
    material.price != null ? material.price : 0.0,
    material.storageRoom || null,
    material.supplier || null,
    material.purchaseDate || null,
    material.expiryDate || null,
    material.status || STATUS.IN_STOCK
  ];
  const result = await db.execute(sql, params);
  return findById(result.insertId);
}

/**
 * 更新物资(部分字段更新)
 */
async function update(id, material) {
  const fieldMap = {
    name: 'name',
    category: 'category',
    specification: 'specification',
    quantity: 'quantity',
    unit: 'unit',
    price: 'price',
    storageRoom: 'storage_room',
    supplier: 'supplier',
    purchaseDate: 'purchase_date',
    expiryDate: 'expiry_date',
    status: 'status'
  };

  const sets = [];
  const params = [];
  for (const [key, column] of Object.entries(fieldMap)) {
    if (material[key] !== undefined && material[key] !== null && material[key] !== '') {
      sets.push(`${column} = ?`);
      params.push(material[key]);
    }
  }
  if (!sets.length) {
    return findById(id);
  }
  params.push(id);
  await db.execute(`UPDATE material SET ${sets.join(', ')} WHERE id = ?`, params);
  return findById(id);
}

/**
 * 删除物资
 */
async function remove(id) {
  const result = await db.execute('DELETE FROM material WHERE id = ?', [id]);
  return result.affectedRows > 0;
}

/**
 * 统计:分类库存汇总
 */
async function statisticsByCategory() {
  return db.query(
    `SELECT category,
            COUNT(*) AS kinds,
            SUM(quantity) AS totalQuantity,
            SUM(quantity * price) AS totalValue
       FROM material
      WHERE status <> 'SCRAPPED'
      GROUP BY category
      ORDER BY totalQuantity DESC`
  );
}

/**
 * 库存预警:数量低于阈值或已过期的物资
 */
async function lowStockWarnings(threshold = 10) {
  return db.query(
    `SELECT * FROM material
      WHERE status <> 'SCRAPPED'
        AND (quantity < ? OR (expiry_date IS NOT NULL AND expiry_date <= CURDATE() + INTERVAL 30 DAY))
      ORDER BY quantity ASC, expiry_date ASC`,
    [threshold]
  );
}

module.exports = {
  STATUS,
  findAll,
  findById,
  create,
  update,
  remove,
  statisticsByCategory,
  lowStockWarnings
};
