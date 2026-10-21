# 学期管理模块 ApiFox 测试用例

> 对应《管理端接口文档》九、学期管理模块（9.1 ~ 9.5）。
> 统一前缀：`http://localhost:8080/api/v1`，环境 profile=dev。
> 9.6 批量导入 / 9.7 下载模板：本版本暂不测（用户要求先不管）。
> 9.1~9.5「仅管理员可见」，需 admin 角色，越权返回 `20005`（HTTP 403）。

---

## 0. 前置：登录获取 Token

1. `GET /auth/captcha` → 拿到 `key` 与 `image`（base64 PNG，解码后识别 4 位验证码）。
2. `POST /auth/login` → 用 A00001 / 123456 + 验证码登录，拿到 `accessToken`。
3. 后续所有请求头加 `Authorization: Bearer <accessToken>`。

> 管理端种子账号：A00001 / A00002（密码 `123456`），userId 为 6 / 7，角色 `admin`。
> 学校：id=1「华中科技大学」（seed_students.sql）。

---

## 种子数据参考（seed_semesters.sql，学校 id=1）

| semesterId | name        | start_date | end_date   | is_current | status |
|:-----------|:------------|:-----------|:-----------|:-----------|:-------|
| 1          | 2024-2025-1 | 2024-09-01 | 2025-01-15 | 0          | 1      |
| 2          | 2024-2025-2 | 2025-02-17 | 2025-07-05 | 0          | 1      |
| 3          | 2025-2026-1 | 2025-09-01 | 2026-01-15 | 0          | 1      |
| 4          | 2025-2026-2 | 2026-02-23 | 2026-07-05 | **1**      | 1      |
| 5          | 2026-2027-1 | 2026-09-01 | 2027-01-15 | 0          | 1      |

> 注意：接口返回的 `name` 是库里的原始值（如 `2024-2025-1`），前端才展示成「2024-2025第一学期」。
> 测试「同名」「日期重叠」时，直接用上表的原始 `name` / 日期即可。

---

## 建议执行顺序

先 9.1 看现有 5 条 → 9.2 造一个测试学期（记下 semesterId）→ 9.3 改它 → 9.5 禁用/启用它 → 9.4 设置当前学期 → 回归 9.1 确认 → 最后删不掉的就不用管（本模块无删除接口）。

---

## 1. 用例：9.1 获取学期列表

**接口：** `GET /admin/semesters`

| 用例 | 请求参数 | 预期结果 |
|------|----------|----------|
| 基础分页 | `page=1&per_page=20` | code=0，`list` 含 semesterId=1~5，`pagination.total=5` |
| 字段校验 | 任意 | 每项含 `semesterId`/`name`/`schoolId`/`schoolName`(华中科技大学)/`startDate`/`endDate`/`isCurrent`/`status`/`statusLabel`/`createdAt`；日期为 `YYYY-MM-DD` |
| 按学校 | `?schoolId=1` | 返回 5 条，全为 schoolId=1 |
| 按学校（无数据） | `?schoolId=99999` | code=0，`list=[]`，`pagination.total=0` |
| 按状态启用 | `?status=1` | 所有 `status=1`，`statusLabel=启用` |
| 按状态禁用 | `?status=0` | 先对某学期执行 9.5 禁用后，该条出现在此处且 `statusLabel=禁用` |
| 组合 | `?schoolId=1&status=1` | 学校 1 下所有启用学期 |
| 分页 | `page=1&per_page=2` | `list` 只有 2 条，`pagination.total=5`，`total_pages=3` |
| 排序 | 基础查询 | `list` 按 `startDate` 倒序：semesterId 顺序大致为 5,4,3,2,1 |
| 越权 | 非 admin 账号（学生/教师） | code=20005，HTTP 403 |

---

## 2. 用例：9.2 创建学期

**接口：** `POST /admin/semesters`

**正常请求体：**

```json
{
  "schoolId": 1,
  "name": "2027-2028-1",
  "startDate": "2027-09-01",
  "endDate": "2028-01-15"
}
```

| 用例 | 请求体 | 预期结果 |
|------|--------|----------|
| 正常创建 | 如上 | code=0，message=创建成功，`data.semesterId` 有值 |
| 缺 schoolId | 去掉 `schoolId` | code=10002（必填参数缺失） |
| 缺 name | 去掉 `name` | code=10002 |
| name 空串 | `"name": "  "` | code=10002 |
| 缺 startDate | 去掉 `startDate` | code=10002 |
| 缺 endDate | 去掉 `endDate` | code=10002 |
| 日期格式错误 | `"startDate": "2027/09/01"` | code=10001（参数错误） |
| 结束不晚于开始 | `startDate=2027-09-01`、`endDate=2027-09-01` | code=10001 |
| 结束早于开始 | `startDate=2027-09-01`、`endDate=2027-08-01` | code=10001 |
| 同名学期 | `name` 用已存在的 `2024-2025-1`（日期合法） | code=3（数据已存在） |
| 日期重叠 | `name=2027-2028-2`、`startDate=2027-01-01`、`endDate=2027-09-01`（与 5 号学期 2026-09-01~2027-01-15 重叠） | code=10001（学期日期与已有学期重叠） |
| 学校不存在 | `schoolId=99999` | code=30001（数据不存在） |
| 默认值 | 正常创建后查 9.1 该条 | `isCurrent=0`、`status=1` |

> **记下返回的 `semesterId`**，后面 9.3 / 9.4 / 9.5 用它（下称「测试学期 ID」）。

---

## 3. 用例：9.3 更新学期（部分更新）

**接口：** `PUT /admin/semesters/{semesterId}`（用上面创建的测试学期 ID）

**请求体（部分更新，传哪个改哪个）：**

```json
{
  "name": "2027-2028-1改",
  "startDate": "2027-09-05",
  "endDate": "2028-01-20"
}
```

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 部分更新 | 如上 | code=0，再查 9.1 该条 `name`/`startDate`/`endDate` 已变 |
| 只改名字 | `{"name": "2027-2028-1新"}` | code=0，日期保持不变 |
| 只改日期 | `{"startDate": "2027-09-10"}` | code=0，name 保持不变 |
| 同名冲突 | `name` 改成 `2024-2025-2`（已存在） | code=3 |
| 日期格式错误 | `startDate=2027-9-1`（非两位） | code=10001 |
| 日期重叠 | 把日期改成与 5 号学期重叠 | code=10001 |
| 学期不存在 | semesterId=99999 | code=30001 |
| 越权 | 非 admin 账号 | code=20005 |

---

## 4. 用例：9.4 设置当前学期

**接口：** `PUT /admin/semesters/{semesterId}/set-current`

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 设置当前学期 | 对 semesterId=3 调用 | code=0，message=设置成功；再查 9.1，仅 semesterId=3 的 `isCurrent=1`，其余（含原 4 号）`isCurrent=0` |
| 换回原当前学期 | 对 semesterId=4 调用 | code=0，仅 4 号 `isCurrent=1` |
| 学期不存在 | semesterId=99999 | code=30001 |
| 越权 | 非 admin 账号 | code=20005 |

---

## 5. 用例：9.5 启用/禁用学期

**接口：** `PUT /admin/semesters/{semesterId}/status`

**请求体：**

```json
{ "status": 0 }
```

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 禁用 | 对测试学期 ID，`status=0` | code=0，message=操作成功；9.1 查该条 `status=0`、`statusLabel=禁用` |
| 启用 | 对测试学期 ID，`status=1` | code=0，`status=1`、`statusLabel=启用` |
| 非法 status | `status=9` | code=10001 |
| 缺 status | 请求体 `{}` | code=10001 |
| 学期不存在 | semesterId=99999 | code=30001 |
| 越权 | 非 admin 账号 | code=20005 |

---

## 6. 通用边界用例

| 用例 | 请求 | 预期结果 |
|------|------|----------|
| 未登录 | 不带 `Authorization` 访问任意 `/admin/semesters*` | code=20001（未登录，HTTP 401） |
| 分页上限 | `per_page=200` | 被后端钳制为 100 |
| 非法页码 | `page=0` | 被钳制为 1 |

---

## 7. 回归验证（关联操作日志）

触发 `POST /admin/semesters`（创建）、`PUT /admin/semesters/{id}`（更新）、
`PUT /admin/semesters/{id}/set-current`、`PUT /admin/semesters/{id}/status` 后，
查 `GET /admin/logs/system?module=semester`，应能查到对应 `action=create/update/set-current/update-status` 的记录，且：

- `relatedType=semester`
- `relatedId` 为该学期 id（`create` 除外，新 id 在返回值里，`relatedId` 为空）
- `description` 已替换变量（如「创建学期: 2027-2028-1」「更新学期: 6」「设置当前学期: 6」）

---

## 附：错误码速查（学期模块）

| code | 含义 | HTTP |
|:-----|:-----|:-----|
| 0     | 成功 | 200 |
| 3     | 数据已存在（同名学期） | 409 |
| 10001 | 参数错误（日期格式/结束不晚于开始/日期重叠/status 非法） | 400 |
| 10002 | 必填参数缺失（schoolId/name/startDate/endDate） | 400 |
| 20001 | 未登录 | 401 |
| 20005 | 无访问权限（非 admin） | 403 |
| 30001 | 数据不存在（学校/学期不存在） | 404 |
