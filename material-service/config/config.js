/**
 * 全局配置(统一从环境变量读取,带默认值)
 */
'use strict';

require('dotenv').config();

module.exports = {
  port: parseInt(process.env.PORT || '8082', 10),
  serviceName: 'material-service',
  serviceVersion: '1.0.0',

  db: {
    host: process.env.DB_HOST || '127.0.0.1',
    port: parseInt(process.env.DB_PORT || '3306', 10),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'lab123456',
    name: process.env.DB_NAME || 'lab_resource_system',
    connectionLimit: parseInt(process.env.DB_POOL_SIZE || '10', 10)
  },

  downstream: {
    gateway: process.env.GATEWAY_URL || 'http://localhost:8080',
    borrowService: process.env.BORROW_SERVICE_URL || 'http://localhost:8081'
  }
};
