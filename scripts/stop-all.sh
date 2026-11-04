#!/usr/bin/env bash
# 停止所有微服务
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "停止服务..."
for port in 8080 8081 8082; do
  PID=$(netstat -ano 2>/dev/null | grep ":$port " | grep LISTENING | awk '{print $5}' | head -1)
  if [ -n "$PID" ]; then
    taskkill //PID $PID //F >/dev/null 2>&1 && echo "  端口 $port (PID $PID) 已停止"
  else
    echo "  端口 $port 无运行中的服务"
  fi
done
