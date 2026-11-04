/**
 * 物资管理微服务入口 (Node.js / Express)
 * ---------------------------------------------------------------
 * 端口: 8082
 * 提供: /materials  REST API
 */
'use strict';

const express = require('express');
const cors = require('cors');
const config = require('../config/config');
const db = require('./db');
const materialController = require('./materialController');

const app = express();

// ---------- 全局中间件 ----------
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 请求日志中间件
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const cost = Date.now() - start;
    console.log(
      `[material-service] ${req.method} ${req.originalUrl} -> ${res.statusCode} (${cost}ms)`
    );
  });
  next();
});

// ---------- 健康检查 ----------
app.get('/health', async (req, res) => {
  let dbState = 'UP';
  try {
    await db.ping();
  } catch (err) {
    dbState = 'DOWN: ' + err.message;
  }
  res.json({
    code: 0,
    message: 'material-service 存活',
    data: {
      service: config.serviceName,
      version: config.serviceVersion,
      port: config.port,
      database: dbState,
      timestamp: new Date().toISOString()
    }
  });
});

// ---------- 业务路由 ----------
app.use('/materials', materialController);

// ---------- 兜底错误处理 ----------
app.use((req, res) => {
  res.status(404).json({
    code: 404,
    message: `未找到路径: ${req.method} ${req.originalUrl}`,
    data: null
  });
});

app.use((err, req, res, next) => {
  console.error('[material-service] 未捕获异常:', err);
  res.status(500).json({
    code: 500,
    message: '服务器内部错误',
    data: null,
    details: err.message
  });
});

// ---------- 启动 ----------
if (require.main === module) {
  const server = app.listen(config.port, () => {
    console.log('==============================================');
    console.log('  实验室资源管理系统 - 物资管理微服务');
    console.log(`  技术栈: Node.js ${process.version} / Express`);
    console.log(`  监听端口: http://localhost:${config.port}`);
    console.log(`  API 入口: http://localhost:${config.port}/materials`);
    console.log(`  数据库: ${config.db.user}@${config.db.host}:${config.db.port}/${config.db.name}`);
    console.log('==============================================');
  });

  process.on('SIGTERM', () => server.close(() => process.exit(0)));
}

module.exports = app;
