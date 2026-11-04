/**
 * 数据库连接池 (mysql2)
 * ---------------------------------------------------------------
 * 物资服务(Node.js)对 MySQL 的封装,提供连接池与查询辅助方法。
 * 所有 SQL 访问都收敛在本文件,便于统一管理与异常处理。
 */
'use strict';

const mysql = require('mysql2/promise');
const config = require('../config/config');

let pool = null;

/**
 * 创建(懒加载)连接池
 */
function getPool() {
  if (!pool) {
    pool = mysql.createPool({
      host: config.db.host,
      port: config.db.port,
      user: config.db.user,
      password: config.db.password,
      database: config.db.name,
      waitForConnections: true,
      connectionLimit: config.db.connectionLimit,
      queueLimit: 0,
      charset: 'utf8mb4',
      timezone: '+08:00',
      dateStrings: true
    });
  }
  return pool;
}

/**
 * 执行查询,返回结果行
 * @param {string} sql  SQL 语句
 * @param {Array}  params  参数占位符
 */
async function query(sql, params = []) {
  const [rows] = await getPool().execute(sql, params);
  return rows;
}

/**
 * 执行写入(INSERT / UPDATE / DELETE),返回 insertId 与 affectedRows
 */
async function execute(sql, params = []) {
  const [result] = await getPool().execute(sql, params);
  return result;
}

/**
 * 事务封装:传入一个接收 connection 的回调函数
 */
async function transaction(callback) {
  const conn = await getPool().getConnection();
  try {
    await conn.beginTransaction();
    const result = await callback(conn);
    await conn.commit();
    return result;
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/**
 * 连通性检查
 */
async function ping() {
  const conn = await getPool().getConnection();
  try {
    await conn.ping();
    return true;
  } finally {
    conn.release();
  }
}

module.exports = { getPool, query, execute, transaction, ping };
