const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const PORT = 3000;

// 静态资源目录
app.use(express.static(path.join(__dirname, 'client')));

// 代理 API 请求到 Java 网关
app.use(['/personnels', '/materials', '/borrows', '/health', '/dashboard'], createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
}));

app.listen(PORT, () => {
    console.log(`==============================================`);
    console.log(`   实验室资源管理系统 - 前端预览代理`);
    console.log(`   访问地址: http://localhost:3000`);
    console.log(`   后端网关: http://localhost:8080`);
    console.log(`==============================================`);
});
