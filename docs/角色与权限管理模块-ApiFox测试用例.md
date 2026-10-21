# 角色与权限管理模块 ApiFox 测试用例

> 对应《管理端接口文档》八、角色与权限管理模块（8.1 ~ 8.7）。
> 统一前缀：`http://localhost:8080/api/v1`，环境 profile=dev。
> 该模块「仅管理员可见」，所有接口需 admin 角色，越权返回 `20005`。

---

## 0. 前置：登录获取 Token

1. `GET /auth/captcha` → 拿到 `key` 与 `image`（base64 PNG，解码后识别 4 位验证码）。
2. `POST /auth/login` → 用 A00001 / 123456 + 验证码登录，拿到 `accessToken`。
3. 后续所有请求头加 `Authorization: Bearer <accessToken>`。

> 管理端种子账号：A00001 / A00002（密码 `123456`），userId 为 6 / 7，角色 `admin`（roleId=2，roleName=超级管理员）。
> 现有系统角色：roleId=1 学生、roleId=2 超级管理员、roleId=3 教师。
> 越权测试账号：任一非 admin 账号（学生/教师），应返回 `20005 无访问权限`。

---

## 建议执行顺序

先 8.7 拿权限 ID → 8.1 看现有角色 → 8.2 造一个测试角色 → 8.3 改它 → 8.5 查它的权限（空）→ 8.6 分配权限 → 8.5 再查确认 → 8.4 删除（先测「被引用角色删不掉」，再测「测试角色删得掉」）。

---

## 1. 用例：8.7 获取权限码列表

**接口：** `GET /admin/permissions`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础查询 | 无 | code=0，返回数组，含 `permissionId`/`permissionCode`/`permissionName`/`status`，如 `indicator:manage`、`log:view` |
| 按模块 | `?module=indicator` | 所有 `permissionCode` 以 `indicator` 开头 |
| 按状态 | `?status=1` | 所有 `status=1` |
| 组合 | `?module=export&status=1` | 前缀 `export` 且启用 |
| 越权 | 非 admin 账号 | code=20005 |

> **记下几个 permissionId**，后面 8.6 要用。

---

## 2. 用例：8.1 获取角色列表

**接口：** `GET /admin/roles`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础分页 | `page=1&per_page=10` | code=0，`list` 含 roleId=1 学生、2 超级管理员、3 教师，`pagination.total>0` |
| 按状态 | `?status=1` | 所有 `status=1`，`statusLabel=启用` |
| 按状态禁用 | `?status=0` | 所有 `status=0` |
| 字段校验 | 任意 | `permissionCount`/`userCount` 为数字；`roleName`/`roleCode`/`level`/`createdAt` 有值 |
| 越权 | 非 admin 账号 | code=20005 |

---

## 3. 用例：8.2 创建角色

**接口：** `POST /admin/roles`

**请求体：**

```json
{
  "roleName": "测试角色",
  "roleCode": "test_role_01",
  "level": 7,
  "description": "测试用角色",
  "status": 1
}
```

| 用例 | 请求体 | 预期结果 |
|------|--------|----------|
| 正常创建 | 如上 | code=0，`data.roleId` 有值，message=创建成功 |
| 缺角色名 | 去掉 `roleName` | code=10002（必填参数缺失） |
| 缺编码 | 去掉 `roleCode` | code=10002 |
| 编码重复 | `roleCode` 用已存在的（如 `admin`） | code=3（数据已存在） |
| 非法 status | `status=9` | code=10001（参数错误） |
| 非法 level | `level=0` | code=10001 |
| 默认值 | 只传 roleName+roleCode | 创建成功，查列表该角色 `level=7`、`status=1` |

> **记下返回的 `roleId`**，后面 8.3/8.5/8.6/8.4 都用它。

---

## 4. 用例：8.3 更新角色

**接口：** `PUT /admin/roles/{roleId}`（roleId 用上面创建出来的）

**请求体（部分更新，传哪个改哪个）：**

```json
{
  "roleName": "测试角色-改",
  "description": "改过描述"
}
```

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 部分更新 | 如上 | code=0，再查列表该角色 `roleName`/`description` 已变 |
| 改编码 | `roleCode` 改成新值 `test_role_02` | code=0，再查列表 `roleCode` 已变 |
| 编码冲突 | `roleCode` 改成 `admin` | code=3（数据已存在） |
| 角色不存在 | roleId=99999 | code=30001（数据不存在） |

---

## 5. 用例：8.5 获取角色权限

**接口：** `GET /admin/roles/{roleId}/permissions`

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 无权限角色 | 用刚创建的测试角色 roleId | code=0，`roleName` 有值，`permissions=[]` |
| 有权限角色 | roleId=2（admin） | `permissions` 非空，每项含 `permissionId`/`permissionCode`/`permissionName` |
| 角色不存在 | roleId=99999 | code=30001 |

---

## 6. 用例：8.6 分配角色权限（覆盖式）

**接口：** `PUT /admin/roles/{roleId}/permissions`

**请求体（permissionId 用 8.7 拿到的真实 ID）：**

```json
{
  "permissionIds": [1, 2]
}
```

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 正常分配 | 给测试角色分配 2 个权限 | code=0，再查 8.5 该角色 `permissions` 恰好 2 条 |
| 覆盖式 | 再分配 1 个（去掉之前的） | code=0，8.5 只返回这 1 条（旧的被删） |
| 清空 | `permissionIds: []` | code=0，8.5 返回 `[]` |
| 非法权限ID | `permissionIds: [99999]` | code=10001（参数错误） |
| 角色不存在 | roleId=99999 | code=30001 |

---

## 7. 用例：8.4 删除角色

**接口：** `DELETE /admin/roles/{roleId}`

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 被用户引用不可删 | roleId=1（学生，有大量用户） | code=30006（数据关联存在），角色仍在 |
| 被用户引用不可删 | roleId=2（admin） | code=30006 |
| 正常删除 | 用前面创建的测试角色 roleId | code=0，再查 8.1 列表该角色已消失 |
| 角色不存在 | roleId=99999 | code=30001 |

---

## 8. 通用边界用例

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 未登录 | 不带 `Authorization` 访问任意 `/admin/roles*`、`/admin/permissions` | code=20001（未登录，401） |
| 分页上限 | `per_page=200` | 被后端钳制为 100 |
| 非法页码 | `page=0` | 被钳制为 1 |

---

## 9. 回归验证（关联操作日志）

- 触发 `POST /admin/roles`（创建）、`PUT /admin/roles/{id}`（更新）、`PUT /admin/roles/{id}/permissions`（分配）后，查 `GET /admin/logs/system?module=role`，应能查到对应 `action=create`/`update`/`assign-permissions` 的记录，且：
  - `relatedType=role`
  - `relatedId` 为该角色 id（`create` 除外，新 id 在返回值里，`relatedId` 为空）
  - `description` 已替换变量（如「创建角色: test_role_01」）