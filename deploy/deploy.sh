#!/usr/bin/env bash
# 服务器端部署脚本：在 /www/wwwroot/studentarchives 目录下运行。
# 用法：
#   ./deploy.sh            # 部署最新版（:latest）
#   ./deploy.sh <git-sha>  # 回滚 / 部署到指定提交对应的镜像版本
set -euo pipefail
cd "$(dirname "$0")"

TAG="${1:-latest}"
export APP_TAG="$TAG"

echo "==> 拉取镜像 student-archives:${TAG}"
docker compose pull app

echo "==> 重启应用容器"
docker compose up -d --no-deps app

echo "==> 等待应用启动"
sleep 20

echo "==> 探活（公开接口 /api/v1/auth/captcha）"
HTTP_CODE=$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/auth/captcha || true)
echo "HTTP_CODE=${HTTP_CODE}"
if [ -z "$HTTP_CODE" ] || [ "$HTTP_CODE" = "000" ]; then
  echo "!! 应用未正常响应，最近日志："
  docker compose logs --tail=80 app
  exit 1
fi

echo "==> 部署完成"
docker compose ps
