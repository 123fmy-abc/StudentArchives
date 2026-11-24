# 卓越学生成长档案系统 · 后端

> 基于 **Spring Boot 3.3 + Java 21** 的单体服务，为学生端、教师端、管理端提供 REST 接口，统一路径前缀 `/api/v1`。

---

## 一、项目简介

- 学生档案、成长记录、成果与奖项、申报审批、指标评分、档案导出、消息通知、组织结构与权限管理等业务全部由本服务承载。
- 鉴权采用 **JWT（Access Token + Refresh Token）**，角色分为学生 / 教师 / 管理员；教师端与管理端共用接口，通过角色权限码控制模块可见性。
- 数据库结构由 **Flyway** 版本化管理（`src/main/resources/db/migration`，当前 51 个迁移脚本，最新版本号 V55）；JPA 仅做 `validate` 校验，不自动改表。
- 配套能力：图形验证码、邮件通知、阿里云 OSS 文件存储、DeepSeek 大模型对话、HTML 模板导出 PDF。

---

## 二、技术栈

| 层级 | 技术选型 | 版本 |
| --- | --- | --- |
| 语言 / 运行时 | Java | 21 |
| 框架 | Spring Boot（Web / Security / Validation / AOP / Actuator） | 3.3.0 |
| 持久层 | Spring Data JPA + Hibernate（`ddl-auto=validate`） | - |
| 数据库 | MySQL | 8.x |
| 迁移 | Flyway（core + mysql） | 随 Boot |
| 缓存 | Redis（Lettuce 连接池）+ Caffeine | - |
| 认证 | Spring Security + JJWT | 0.12.6 |
| 邮件 | Spring Boot Mail + Spring Retry | - |
| 文件存储 | 阿里云 OSS SDK | 3.17.4 |
| 文档导出 | OpenPDF / openhtmltopdf / Mustache | 1.3.30 / 1.0.10 / 0.9.13 |
| 配置注入 | spring-dotenv（加载根目录 `.env`） | 4.0.0 |
| 其他 | Lombok、Maven Wrapper | - |

---

## 三、快速开始

### 环境要求

- JDK 21
- Maven 3.9+（也可直接用仓库内的 `mvnw` / `mvnw.cmd`）
- MySQL 8（可连团队共享库，或用 `docker-compose.yml` 本地起库）
- Redis（默认连本地，用于验证码、令牌等缓存）

### 启动步骤

```bash
# 1. 准备个人配置（.env 已被 .gitignore 忽略）
#    Windows
copy .env.example .env
#    macOS / Linux
cp .env.example .env

# 2. 编辑 .env，至少填写 DB_PASSWORD、MAIL_USERNAME、MAIL_PASSWORD、OSS_*、JWT_SECRET

# 3. 启动（默认 profile = dev）
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

启动后：

- 服务地址：`http://localhost:8080/api/v1`
- 健康检查：`http://localhost:8080/api/v1/actuator/health`

### 本地起 MySQL（可选）

根目录 `docker-compose.yml` 会拉起 MySQL 8 与应用容器。注意必须显式提供强口令，否则 compose 直接拒绝启动：

```bash
# 在 .env 中设置 MYSQL_ROOT_PASSWORD、MYSQL_PASSWORD 后
docker compose up -d mysql
```

MySQL 仅绑定 `127.0.0.1:3306`，不对公网开放。

---

## 四、目录结构

```
backend/
├── src/main/java/com/example/studentarchives/
│   ├── controller/                 # REST 接口（Fmy、Lzw 为成员分工子包）
│   ├── service/                    # 业务实现（common / schedule / Fmy / Lzw）
│   ├── repository/                 # Spring Data JPA 仓储（含 projection）
│   ├── entity/                     # 实体，按域拆分：ai / approval / archive / award /
│   │                               #   career / evaluation / export / file / grade /
│   │                               #   growth / log / message / org / schedule / user ...
│   ├── dto/                        # 请求与响应 DTO
│   ├── enums/                      # 业务枚举
│   ├── exception/                  # 全局异常与错误码
│   ├── config/                     # Security、CORS、OSS、DeepSeek、Jackson 等配置
│   ├── common/                     # 统一响应、分页等公共模型
│   ├── event/ listener/            # 领域事件与监听（如消息通知）
│   ├── annotation/ util/ support/   # 自定义注解、工具、支撑组件（如 CaptchaSkipPolicy）
│   └── StudentArchivesApplication.java
├── src/main/resources/
│   ├── application.yml             # 公共配置（端口、context-path、JPA、Flyway、JWT、OSS、DeepSeek）
│   ├── application-dev.yml         # 开发：共享 MySQL / 本地 Redis / QQ 邮箱
│   ├── application-test.yml        # 测试：继承 dev，仅追加免验证码白名单
│   ├── application-prod.yml        # 生产：全部走环境变量，强制关闭免验证码
│   ├── logback-spring.xml          # 日志配置（LOG_PATH 默认 ./logs）
│   └── db/migration/               # Flyway 迁移脚本（当前 51 个，最新 V55）
├── sql/                            # 手工种子 / 修复脚本（非 Flyway 自动执行）
├── docs/                           # 三端接口文档、表结构、补充接口文档
├── deploy/                         # 宝塔服务器部署脚本与 compose
├── .github/workflows/ci.yml        # CI：JDK 21 编译校验
├── Dockerfile                      # 多阶段构建（运行阶段含中文字体）
├── docker-compose.yml              # 本地 MySQL + app
├── deploy-local.ps1                # 本地一键部署（构建 → 上传 → 重启 → 健康检查 → 回滚）
├── .env.example                    # 环境变量模板
├── mvnw / mvnw.cmd
└── pom.xml
```

---

## 五、配置说明

### Profile

| Profile | 用途 | 关键差异 |
| --- | --- | --- |
| `dev`（默认） | 日常开发 | 连接团队共享 MySQL（`8.137.191.26`），Redis 连本机；Flyway 允许 `out-of-order` |
| `test` | E2E 自动化 | 通过 `spring.profiles.group` 同时激活 `dev`，只添加免验证码白名单 |
| `prod` | 生产 | 数据库 / Redis / 邮件 / JWT 全部由环境变量注入；免验证码被硬编码禁用 |

切换方式：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
java -jar target/student-archives-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 环境变量（`.env`）

| 变量 | 是否必填 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` | 否 | 默认 `8.137.191.26` / `3306` / `student_archives` / `StuArch` |
| `DB_PASSWORD` | **是** | 数据库密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | 否 | 开发默认 `127.0.0.1` / `6380` / `123456` / `0`；本地 Redis 为 6379 时需覆盖端口 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | **是** | QQ 邮箱与 SMTP 授权码（非登录密码） |
| `OSS_ENDPOINT` / `OSS_REGION` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET_NAME` | **是** | 文件上传依赖；`region` 为 V4 签名必需 |
| `DEEPSEEK_API_KEY` | 否 | 未配置时 AI 对话返回兜底话术 |
| `JWT_SECRET` | **是** | Base64 编码的 HMAC-SHA256 密钥（≥256 位）；更换后全部令牌失效 |
| `CAPTCHA_SKIP_SECRET` | 否 | 免验证码密钥，留空即整体失效；需与前端 `VITE_CAPTCHA_SKIP_SECRET` 一致 |
| `CORS_ALLOWED_ORIGINS` | 生产必填 | 逗号分隔；开发默认放行 `localhost/127.0.0.1` 的 3000 / 5173 / 8080 |
| `STUDENT_EXPORT_FONT_PATH` | 否 | 导出 PDF 的中文字体路径，空则自动探测系统字体 |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | 用 compose 时必填 | 未设置会导致 compose 启动失败（有意设计） |

### 关键公共配置（`application.yml`）

- 端口 `8080`，`server.servlet.context-path: /api/v1`
- Access Token 24 小时、Refresh Token 7 天；勾选「记住我」时为 7 天 / 30 天
- 单文件上传上限 12MB（OSS 上限 10MB）
- 文件上传 `OSS.max-file-size` 10MB、临时文件 24 小时、签名 URL 30 分钟
- 日志输出到 `./logs`（容器内为 `/var/log/student-archives`）

---

## 六、数据库与迁移

- **Flyway 是唯一的建表 / 改表入口**：`src/main/resources/db/migration/V{n}__{描述}.sql`，启动时自动执行。新增结构请新建 `V52__xxx.sql`，**不要修改已执行过的历史脚本**（`validate-on-migrate` 会直接阻止启动）。
- JPA `ddl-auto: validate` 只校验实体与表是否一致，不会自动改表。
- `sql/` 下为手工执行的数据脚本（`seed_*` 初始化数据、`repair_*` 数据修复），不参与自动迁移。
- 开发环境允许乱序迁移（`out-of-order: true`）并忽略缺失脚本，方便多人并行开发。

---

## 七、认证、权限与安全

### JWT 流程

1. `GET /api/v1/auth/captcha` 获取图形验证码，`POST /api/v1/auth/login` 登录。
2. 请求体 `loginType` 取 `student` 或 `admin`（教师与管理员共用 `admin` 入口），后端再按账号实际角色校验，不匹配会返回对应提示。
3. 返回 Access Token（24h）与 Refresh Token（7d），前端通过 `Authorization: Bearer <token>` 访问接口；`POST /auth/refresh` 以 Rotation 方式续期。
4. 首次登录或重置密码后进入「强制改密」状态，仅放行改密相关接口。
5. `POST /auth/logout` 通过递增 `tokenVersion` / `refreshTokenVersion` 使令牌失效，可选「退出所有设备」。

### 角色与权限

- 角色编码存于 `roles.code`：`student` / `teacher` / `admin`，并配合角色等级与数据范围（`scope_types`）使用。
- 权限码采用 `域:资源:动作` 命名（如 `student:archive:view`、`export:manage`），通过 `role_permissions` 关联；接口按所需权限码校验。
- `admin` 角色直通教师端模块校验；前端可见模块由登录后返回的权限集合驱动。

### 图形验证码与免验证码

- 验证码有效期 180 秒，前端倒计时读取接口返回的 `expiresIn`。
- 免验证码**仅限 dev / test**，需同时满足：当前所有 active profile 都在白名单、配置完整、请求头 `X-Captcha-Skip-Token` 密钥正确、账号在 `skip-users` 内。
- `prod` / `production` / `prd` 在 `CaptchaSkipPolicy` 中有硬编码守卫，任何参数都无法开启。
- 测试白名单默认：`202401002`、`T00003`、`A00002`（dev 另含 `A00001`）。

### 其他安全约束

- CORS 白名单由 `CORS_ALLOWED_ORIGINS` 控制，生产禁止通配符与凭证并存。
- 数据库端口不对公网开放，应用使用仅授权单库的独立账号。
- 密钥只放 `.env` 与部署 Secrets，不入库；`.env` 已加入 `.gitignore`。

---

## 八、接口文档

服务未集成 Swagger，接口契约以仓库内文档为准：

| 文档 | 内容 |
| --- | --- |
| [docs/学生端接口文档.md](./docs/学生端接口文档.md) | 学生端全部接口 |
| [docs/教师端接口文档.md](./docs/教师端接口文档.md) | 教师端全部接口 |
| [docs/管理端接口文档.md](./docs/管理端接口文档.md) | 管理端接口与权限 |
| [docs/Project_补充接口文档_20261117.md](./docs/Project_补充接口文档_20261117.md) | 近期补充接口 |
| [docs/学生档案系统表.md](./docs/学生档案系统表.md) | 数据库表结构说明 |

通用约定：

- 基础路径 `/api/v1`，统一响应体为 `{ code, message, data, trace_id, timestamp }`，`code = 0` 表示成功。
- 时间字段使用 ISO 8601，时区 `Asia/Shanghai`。
- 文件上传单请求上限 12MB，OSS 对象上限 10MB，返回的访问地址为限时签名 URL。

---

## 九、本地联调

- 前端开发服务器默认 `http://localhost:5173`，通过 Vite 代理把 `/api` 转发到 `http://111.228.56.207:8089`（已部署后端）。
- 若前端直连本机后端，请把代理目标改为 `http://localhost:8080`，并确认前端端口在 CORS 白名单内（3000 / 5173 / 8080）。
- 登录验证码、免验证码联调需前后端 `CAPTCHA_SKIP_SECRET` / `VITE_CAPTCHA_SKIP_SECRET` 保持一致，且账号在后端 `skip-users` 内。

---

## 十、构建、测试与部署

### 构建

```bash
./mvnw clean package -DskipTests
# 产物：target/student-archives-0.0.1-SNAPSHOT.jar
```

### 测试

- `src/test` 当前为空，仓库内没有后端自动化测试；CI 只做编译校验。
- 回归依赖前端 Playwright E2E 与 `test` profile 的免验证码链路。

### CI

`.github/workflows/ci.yml`：push / PR 到 `main` 时用 JDK 21 执行 `./mvnw -DskipTests package`，拦截编译错误，不做部署。

### 部署

- **本地一键部署（当前主用）**：
  ```bash
  powershell -ExecutionPolicy Bypass -File deploy-local.ps1
  ```
  流程：本地构建 → `scp` 上传 jar 与 `deploy/deploy-jar.sh` → 服务器备份、替换、重启、健康检查，失败自动回滚。
- **服务器侧容器部署**：`deploy/` 下提供 `docker-compose.yml`、`deploy.sh`、`.env.example`，详见 [deploy/README.md](./deploy/README.md)。
- **镜像构建**：根目录 `Dockerfile` 采用多阶段构建，运行阶段基于 `eclipse-temurin:21-jre-alpine`，安装 `font-noto-cjk` 以支持 PDF 中文字体，默认以 `prod` profile 启动。
- 部署后探活地址：`http://<host>:8080/api/v1/actuator/health`。

---

## 十一、常用命令

| 命令 | 说明 |
| --- | --- |
| `./mvnw spring-boot:run` | 以 dev profile 启动 |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=test` | 以 test（含 dev）启动，用于 E2E |
| `./mvnw clean package -DskipTests` | 打包 fat jar |
| `./mvnw clean package` | 打包并跑测试（当前无测试用例） |
| `java -jar target/student-archives-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod` | 生产方式启动 |
| `docker compose up -d` | 本地拉起 MySQL + 应用 |
| `powershell -ExecutionPolicy Bypass -File deploy-local.ps1` | 一键部署到服务器 |

---

## 十二、相关文档

- [deploy/README.md](./deploy/README.md) —— 服务器准备、镜像仓库、部署与回滚、安全基线
- [docs/学生端接口文档.md](./docs/学生端接口文档.md)
- [docs/教师端接口文档.md](./docs/教师端接口文档.md)
- [docs/管理端接口文档.md](./docs/管理端接口文档.md)
- [docs/学生档案系统表.md](./docs/学生档案系统表.md)
- [.env.example](./.env.example) —— 环境变量模板
