# 跨语言调用的实验室资源管理系统

浏览器只访问 Java 网关 `http://localhost:8080`。人员数据由网关直接读写 MySQL；物资和借用分别由 Node.js、Python 提供，网关用 HTTP 转发。

| 模块 | 目录 | 端口 | 技术 |
|---|---|---|---|
| 网关 / 人员 / 页面 | `gateway-java` | 8080 | Java 8、Spring Boot、JdbcTemplate |
| 物资 | `material-service` | 8082 | Node.js、Express、mysql2 |
| 借用 | `borrow-service` | 8081 | Python、Flask、PyMySQL |
| 页面原稿 | `client/index.html` | — | 静态页面，运行时由网关提供 |
| 数据库脚本 | `db/init.sql` | 3306 | MySQL 8，库名 `lab_resource_system` |

接口说明见 `docs/API接口文档.md`。

## 准备

1. 安装 JDK 8、Maven、Node.js、Python 3 和 MySQL 8。
2. 复制环境变量示例并填写本机数据库密码：

```bash
copy material-service\.env.example material-service\.env
copy borrow-service\.env.example borrow-service\.env
```

3. 安装依赖：

```bash
cd material-service && npm install
cd ../borrow-service && pip install -r requirements.txt
```

## 启动

Windows：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
```

脚本会执行 `db/init.sql`，再启动 8082、8081、8080。数据库密码从环境变量 `DB_PASSWORD` 或 `material-service/.env` 读取，不要写进代码。

打开 `http://localhost:8080/`。健康检查为 `http://localhost:8080/health`。

停止：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
```
