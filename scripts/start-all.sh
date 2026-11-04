#!/usr/bin/env bash
# ============================================================
# 一键启动三个微服务 + 数据库初始化
# 用法: bash scripts/start-all.sh
# ============================================================
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "项目根目录: $ROOT"

# ---------- 1. 初始化数据库 ----------
echo ""
echo "[1/4] 初始化 MySQL 数据库 ..."
if command -v mysql >/dev/null 2>&1; then
  DB_USER="${DB_USER:-root}"
  DB_PASS="${DB_PASSWORD:-lab123456}"
  mysql -u "$DB_USER" -p"$DB_PASS" < "$ROOT/db/init.sql" && echo "  数据库初始化完成"
else
  echo "  未找到 mysql 命令,请手动执行 db/init.sql"
fi

# ---------- 2. Node.js 物资服务 (8082) ----------
echo ""
echo "[2/4] 启动 Node.js 物资管理服务 (:8082) ..."
cd "$ROOT/material-service"
[ -f .env ] || cp .env.example .env
[ -d node_modules ] || npm install
nohup node src/app.js > "$ROOT/scripts/material-service.log" 2>&1 &
echo "  PID: $!  日志: scripts/material-service.log"

# ---------- 3. Python 借用服务 (8081) ----------
echo ""
echo "[3/4] 启动 Python 借用记录服务 (:8081) ..."
cd "$ROOT/borrow-service"
[ -f .env ] || cp .env.example .env
nohup python app.py > "$ROOT/scripts/borrow-service.log" 2>&1 &
echo "  PID: $!  日志: scripts/borrow-service.log"

# ---------- 4. Java 网关 (8080) ----------
echo ""
echo "[4/4] 打包并启动 Java 网关服务 (:8080) ..."
cd "$ROOT/gateway-java"
MVN_CMD=""
if command -v mvn >/dev/null 2>&1; then
  MVN_CMD="mvn"
else
  MVN_CMD=$(find "$HOME/.m2/wrapper/dists" -name mvn -type f 2>/dev/null | head -1)
fi
if [ -n "$MVN_CMD" ]; then
  "$MVN_CMD" -q clean package -DskipTests
  nohup java -jar target/gateway-java.jar > "$ROOT/scripts/gateway-java.log" 2>&1 &
  echo "  PID: $!  日志: scripts/gateway-java.log"
else
  echo "  未找到 mvn,请手动构建并启动"
fi

# ---------- 等待就绪 ----------
echo ""
echo "等待服务启动(约 25 秒)..."
sleep 25

echo ""
echo "=============================================="
echo "  服务健康检查"
echo "=============================================="
for u in "http://localhost:8080/health" "http://localhost:8082/health" "http://localhost:8081/health"; do
  printf "  %-35s -> " "$u"
  curl -s -m 5 "$u" | head -c 200 || echo "(未响应)"
  echo ""
done
echo ""
echo "客户端页面: client/index.html  (用浏览器打开)"
echo "统一入口:   http://localhost:8080"
