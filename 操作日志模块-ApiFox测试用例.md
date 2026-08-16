# 操作日志模块 ApiFox 测试用例

> 对应《管理端接口文档》四、操作日志模块（4.1 ~ 4.3）。
> 统一前缀：`http://localhost:8080/api/v1`，环境 profile=dev。

---

## 0. 前置：登录获取 Token

1. `GET /auth/captcha` → 拿到 `key` 与 `image`（base64 PNG，解码后肉眼识别 4 位验证码）。
2. `POST /auth/login` → 用 A00001 / 123456 + 验证码登录，拿到 `token`。
3. 后续所有 `/admin/*` 请求头加 `Authorization: Bearer <token>`。

> 管理端种子账号：A00001 / A00002（密码 `123456`），userId 为 6 / 7，角色 `admin`（roleId=2）。
> 越权测试账号：任一非管理员、且无 `log:view` 权限的账号。

---

## 1. 造测试数据

三个查询接口依赖不同表，先按需造数：

| 目标表 | 如何造数 | 触发接口 |
|--------|----------|----------|
| `login_logs`（4.2） | 登录即自动写入 | `POST /auth/login`（任意账号） |
| `system_logs`（4.1） | 触发带 `@AuditLog` 的写接口 | `POST /admin/indicators` / `POST /admin/export-templates` / `POST /admin/ability-dimensions` / 用户管理写接口（见下） |
| `export_operation_logs`（4.3） | 触发研究数据导出 | `POST /admin/exports/research` |

**用户管理写接口（本次已补 `@AuditLog`，module=`user`）也能给 4.1 造数：**

| action | 接口 |
|--------|------|
| `create` | `POST /admin/users` |
| `update` | `PUT /admin/users/{userId}` |
| `update-status` | `PUT /admin/users/{userId}/status` |
| `reset-password` | `PUT /admin/users/{userId}/password/reset` |
| `update-roles` | `PUT /admin/users/{userId}/roles` |
| `update-scopes` | `PUT /admin/users/{userId}/scopes` |

---

## 2. 用例：4.1 查询系统操作日志

**接口：** `GET /admin/logs/system`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础分页 | `page=1&per_page=10` | code=0，`list` 有数据，`pagination.total>0` |
| 按操作人 | `operatorId=7` | 所有 `operatorId` 均为 7 |
| 按角色 | `roleId=2` | 所有 `roleId=2`，`roleName=系统管理员` |
| 按操作类型 | `action=create` | 所有 `action=create` |
| 按模块 | `module=user` | 所有 `module=user`（用户管理操作） |
| 按级别 | `logLevel=3` | 所有 `logLevel=3`（审计） |
| 按关联类型/ID | `relatedType=...&relatedId=...` | 匹配的记录 |
| 时间范围 | `startTime=2026-07-01T00:00:00+08:00&endTime=2026-08-15T23:59:59+08:00` | `createdAt` 落在区间内 |
| 组织维度-年级 | `grade=2024级` | 仅被操作学生属该年级的日志 |
| 组织维度-学院 | `collegeId=1` | 仅被操作学生属该学院的日志 |
| 组织维度-班级 | `classId=1` | 仅被操作学生属该班级的日志 |
| 组织维度组合 | `majorId=1&classId=1` | 两者交集 |
| 字段校验 | 任意 | `operatorName` 有值；`afterData` 为 JSON 对象（非字符串）；`createdAt` 带时区 |
| 越权 | 无 `log:view` 账号 | code=20005（HTTP 403） |
| 空结果 | `action=not-exist` | code=0，`list=[]`，`total=0` |

---

## 3. 用例：4.2 查询登录日志

**接口：** `GET /admin/logs/login`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础分页 | `page=1&per_page=10` | code=0，有登录记录 |
| 按用户 | `userId=7` | 所有 `userId=7` |
| 按登录状态-成功 | `loginStatus=1` | 所有 `loginStatus=1` |
| 按登录状态-失败 | `loginStatus=0` | 所有 `loginStatus=0` |
| 按 IP | `ipAddress=127.0.0.1` | `ipAddress` 匹配 |
| 时间范围 | `startTime=...&endTime=...` | `loginAt` 落在区间内 |
| 字段校验 | 任意 | `userName` 有值；`loginAt` 有值；`logoutAt=null` |

---

## 4. 用例：4.3 查询导出操作日志

**接口：** `GET /admin/logs/exports`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础分页 | `page=1&per_page=10` | code=0，有导出记录（需先触发导出） |
| 按操作人 | `operatorId=7` | 所有 `operatorId=7` |
| 按导出类型 | `exportType=archive_research` | 匹配 |
| 按匿名化 | `isAnonymized=0` / `isAnonymized=1` | 匹配 |
| 时间范围 | `startTime=...&endTime=...` | `createdAt` 落在区间内 |
| 字段校验 | 任意 | `exportJobId` / `downloadedAt` / `ipAddress` 为 `null`；`filterConditions` 为 JSON 对象；`operatorName`、`roleName` 有值 |

---

## 5. 通用边界用例

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 未登录 | 不带 `Authorization` 头访问任意 `/admin/logs/*` | code=20001（未登录，401） |
| 分页上限 | `per_page=200` | 被后端钳制为 100 |
| 非法页码 | `page=0` | 被钳制为 1 |
| 非法时间 | `startTime=abc` | 不报错，按无时间过滤处理 |

---

## 6. 回归验证

- 触发一次 `PUT /admin/users/{userId}/status`（禁用用户）后，立即查 `GET /admin/logs/system?module=user&action=update-status`，应能查到 `statusLabel=成功`、`description` 含目标 `userId` 的记录。
- 触发 `PUT /admin/users/{userId}/password/reset` 后，`beforeData` 应为 `null`（`logParams=false`，避免密码落库）。