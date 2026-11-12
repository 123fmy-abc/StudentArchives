#!/usr/bin/env bash
# 服务器端部署脚本（jar + systemd 方案，与宝塔实际部署形态一致）。
# 流程：备份旧 jar → 替换 → systemctl restart → 健康检查 → 失败自动回滚。
# 用法：bash deploy-jar.sh <新jar路径>
#
# 若实际路径不同，改下面 4 个变量即可（或用环境变量覆盖）：
#   JAR_PATH=/xxx.jar SERVICE=xxx PORT=8086 bash deploy-jar.sh <新jar>
set -euo pipefail

APP_NAME="student-archives"
JAR_PATH="${JAR_PATH:-/www/wwwroot/studentachievement/student-archives-0.0.1-SNAPSHOT.jar}"
SERVICE="${SERVICE:-spring_student-archives}"
PORT="${PORT:-8086}"
HEALTH_URL="http://127.0.0.1:${PORT}/api/v1/auth/captcha"
BACKUP_DIR="${BACKUP_DIR:-$(dirname "$JAR_PATH")/backups}"
KEEP_BACKUPS=5

new_jar="${1:-}"
if [ -z "$new_jar" ] || [ ! -f "$new_jar" ]; then
  echo "!! 找不到新 jar：${new_jar:-<未指定>}"
  exit 1
fi
if [ ! -f "$JAR_PATH" ]; then
  echo "!! 找不到当前 jar：$JAR_PATH"
  echo "   请先在服务器执行 systemctl cat ${SERVICE} | grep ExecStart"
  echo "   确认真实 jar 路径后，用 JAR_PATH=<真实路径> 重新运行，或修改本脚本顶部变量。"
  exit 1
fi

ts=$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"

echo "==> 1/4 备份当前版本"
cp -p "$JAR_PATH" "$BACKUP_DIR/${APP_NAME}-${ts}.jar"
ls -1t "$BACKUP_DIR"/${APP_NAME}-*.jar 2>/dev/null | tail -n +$((KEEP_BACKUPS + 1)) | xargs -r rm -f
echo "    已备份到 $BACKUP_DIR/${APP_NAME}-${ts}.jar（保留最近 ${KEEP_BACKUPS} 份）"

echo "==> 2/4 替换 jar"
install -m 0644 "$new_jar" "$JAR_PATH"

echo "==> 3/4 重启服务 ${SERVICE}"
systemctl restart "$SERVICE"

echo "==> 4/4 健康检查 ${HEALTH_URL}（最多等 120 秒）"
code=000
for _ in $(seq 1 24); do
  sleep 5
  code=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)
  if [ -n "$code" ] && [ "$code" != "000" ]; then
    break
  fi
done

if [ -z "$code" ] || [ "$code" = "000" ]; then
  echo "!! 健康检查失败，服务未响应。最近日志："
  # 宝塔 nohup 启动把输出重定向到应用日志，journal 里通常只有启停记录，两处都打
  tail -n 60 /www/wwwlogs/java/springboot/student-archives.log 2>/dev/null || true
  journalctl -u "$SERVICE" -n 30 --no-pager || true
  echo "!! 自动回滚到上一版本"
  cp -p "$BACKUP_DIR/${APP_NAME}-${ts}.jar" "$JAR_PATH"
  systemctl restart "$SERVICE"
  sleep 10
  rollback_code=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)
  echo "!! 回滚完成，回滚后健康检查 HTTP ${rollback_code}。请根据上面的日志排查新版本问题。"
  exit 1
fi

echo "==> 部署成功：$(systemctl is-active ${SERVICE})，健康检查 HTTP ${code}"
