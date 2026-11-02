# 自动化部署说明（运维）

本目录包含部署到宝塔服务器所需的全部文件，配合仓库根目录的
`.github/workflows/deploy.yml`，实现「push 到 main → 自动构建 → 自动部署」。

## 一、服务器准备（一次性）

1. 宝塔面板「软件商店」安装 **Docker 管理器**。
2. 创建部署目录并放入本目录文件：
   ```bash
   mkdir -p /www/wwwroot/studentarchives
   # 把 deploy/ 下的 docker-compose.yml、deploy.sh 上传到该目录
   ```
3. 准备环境变量文件（密钥都放这里，不提交仓库）：
   ```bash
   cp .env.example .env
   vim .env          # 填写真实值
   chmod 600 .env
   ```
4. 给部署脚本执行权限：`chmod +x deploy.sh`

## 二、镜像仓库（阿里云 ACR）

1. 阿里云「容器镜像服务 ACR」创建**个人版**实例 + 命名空间（如 `studentarchives`）。
2. 记录三样：Registry Host（如 `registry.cn-hangzhou.aliyuncs.com`）、命名空间、访问凭证用户名/密码。

## 三、GitHub 侧

1. 生成部署密钥：
   ```bash
   ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy_key
   ```
   将公钥 `deploy_key.pub` 内容追加到服务器 `~/.ssh/authorized_keys`。
2. 仓库 Settings → Secrets and variables → Actions，配置以下 Secrets：
   - `ACR_HOST` / `ACR_NAMESPACE` / `ACR_USERNAME` / `ACR_PASSWORD`
   - `SSH_HOST` / `SSH_PORT` / `SSH_USER` / `SSH_PRIVATE_KEY`（私钥全文）
3. 推送 main 分支即触发；也可在 Actions 页手动 Run workflow。

## 四、日常使用

- 改代码 → `git push origin main` → 自动完成构建 + 部署 + 探活。
- 手动部署最新版：服务器上 `./deploy.sh`。
- 回滚到某次提交：`./deploy.sh <git-sha>`（流水线已为每次构建打上 sha 标签）。

## 五、注意事项

- 改数据库结构：**新增** `V47__xxx.sql`（不要修改已有的历史迁移），Flyway 启动时自动迁移。
- 密钥只放在 GitHub Secrets 与服务器 `.env`，绝不提交进仓库。
- MySQL / Redis 用宝塔原生服务，本部署只重建 app 容器，不触碰数据库。
- 应用统一前缀为 `/api/v1`（`server.servlet.context-path`），Nginx 反代 / 探活都要带此前缀。

## 六、安全基线（发布前必须满足）

- **数据库端口不对公网开放**：MySQL 3306 只监听 `127.0.0.1`，云安全组/宝塔防火墙不得放行 3306；公网扫描必须不可达。
- **禁止 root 远程登录**：root 仅在服务器本机使用；应用使用独立账号（如 `StuArch`），仅授予 `student_archives` 单库权限。
- **强口令并定期轮换**：`DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` 不得使用默认值（如 `root123`），通过 `.env` 注入并定期更换。
- **密钥轮换**：更换 `JWT_SECRET` 会使全部已签发令牌失效，属预期行为。
- **验证方式**：外网 `nc -vz <公网IP> 3306` 应超时/拒绝；`SELECT user,host FROM mysql.user;` 确认无 `root@'%'`；应用账号仅能看到业务库。

