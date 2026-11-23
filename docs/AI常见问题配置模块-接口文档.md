# 学生档案管理系统 - AI 常见问题（FAQ）配置模块接口文档（V1.0）

> 模块定位：把 AI 助手聊天页底部“常见问题”快捷入口由前端硬编码改为**管理员自主配置**。
> 关联版本：Flyway `V59__ai_faq_questions.sql`；管理端接口文档 V5.15 第十九章；学生端接口文档 V5.22 第 9.10 节。
> 更新日期：2026-11-23

------

## 一、模块概述

### 1.1 背景

AI 助手聊天页（`/ai`）底部此前展示一组固定问题（如“如何填写个人档案信息？”“奖项申报需要准备哪些材料？”）。问题内容与学期、年级、学校政策强相关，硬编码无法维护。

本模块提供：

- 管理端：常见问题的列表、创建、更新（含启用/禁用、排序）、软删除；
- 学生端：只读获取启用中的问题列表，用于渲染快捷按钮。

管理员可直接配置如“帮我总结我的大一生活”“我未来适合做什么职业”“我下学期应该多往哪方面发展”等面向学生成长的高价值提问，引导用户使用 AI 能力。

### 1.2 交互链路

```
管理员                 管理端                  数据库                 学生端
  │  POST/PUT/DELETE     │                      │                      │
  ├─────────────────────>│  ai_faq_questions    │                      │
  │                      ├─────────────────────>│                      │
  │                      │                      │   GET /ai/faqs       │
  │                      │                      │<─────────────────────┤
  │                      │                      │  仅 status=1 且未删除 │
  │                      │                      │─────────────────────>│
  │                      │                      │       渲染快捷按钮     │
  │                      │                      │  点击 → 作为 content  │
  │                      │                      │  调 9.4.1 发送消息     │
```

**关键设计：FAQ 不新增对话链路。** 用户点击问题后，前端把问题文本原样作为 `content` 调用既有发送接口（学生端 9.4.1 流式 / 9.4 非流式），服务端不做任何特殊识别，因此不影响历史会话、消息表结构与 AI 调用日志。

### 1.3 权限与作用域

| 项目 | 说明 |
| :--- | :--- |
| 管理端权限码 | `ai:faq:manage`（V59 写入 `permissions` 并授予 `admin` 角色） |
| 鉴权方式 | `AdminAuthService.requireAdminOrPermission(userId, "ai:faq:manage")`，越权返回 `20005`（HTTP 403） |
| 学生端鉴权 | 登录即可访问（遵循全局鉴权规范） |
| 作用域 | 全局配置，无 `school_id`，所有学校共用一份列表 |

### 1.4 涉及代码

| 层 | 文件 |
| :--- | :--- |
| 迁移 | `src/main/resources/db/migration/V59__ai_faq_questions.sql` |
| 实体 | `entity/ai/AiFaqQuestion.java` |
| 持久层 | `repository/AiFaqQuestionRepository.java` |
| DTO | `dto/Fmy/aifaq/request/AdminAiFaqCreateRequest.java`、`AdminAiFaqUpdateRequest.java`、`dto/Fmy/aifaq/response/AiFaqQuestionResponse.java` |
| 管理端 | `controller/Fmy/AdminAiFaqController.java`、`service/Fmy/AdminAiFaqService.java` |
| 学生端 | `service/Lzw/AiFaqQueryService.java`、`controller/Lzw/AiConversationController.java`（`GET /ai/faqs`） |

------

## 二、数据库设计

### 2.1 `ai_faq_questions`

```sql
CREATE TABLE `ai_faq_questions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `question`         VARCHAR(200)    NOT NULL COMMENT '常见问题内容（点击后作为消息发送）',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`           INT             NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_afq_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手常见问题配置表';
```

| 字段 | 类型 | 可空 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint unsigned | ❌ | 主键 |
| `question` | varchar(200) | ❌ | 问题内容，即点击后发送的文本；允许重复 |
| `sort` | int unsigned | ❌ | 排序，升序展示 |
| `status` | int | ❌ | 0=禁用 1=启用 |
| `created_at` / `updated_at` | timestamp | ❌ | 审计时间（`BaseEntity` 自动维护） |
| `deleted_at` | timestamp | ✅ | 软删除时间（`@SQLRestriction` 过滤） |
| `is_deleted_null` | tinyint(1) | ✅ | 生成列，兼容 MySQL 条件唯一索引（本表未建唯一键） |

### 2.2 种子数据

| ID | question | sort | status |
| :--- | :--- | :--- | :--- |
| 1 | 帮我总结我的大一生活 | 1 | 1 |
| 2 | 我未来适合做什么职业 | 2 | 1 |
| 3 | 我下学期应该多往哪方面发展 | 3 | 1 |

> 种子使用固定 ID + `INSERT IGNORE`，迁移可重复执行且不会覆盖管理员后续修改。

### 2.3 权限种子

```sql
INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`)
SELECT 'AI常见问题配置', 'ai:faq:manage', 3, NULL, 9, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `permissions` p WHERE p.`code` = 'ai:faq:manage' AND p.`deleted_at` IS NULL
);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'ai:faq:manage' AND p.`deleted_at` IS NULL
WHERE r.`deleted_at` IS NULL
  AND r.`code` = 'admin';
```

------

## 三、管理端接口

> 统一前缀 `/admin/ai-faqs`；统一响应结构、分页与幂等规范见《学生端接口文档》全局规范。
> 列表**不分页**（配置项数量通常 < 30），一次性返回数组。

### 3.1 获取常见问题列表

**接口地址：** `GET /admin/ai-faqs`

**请求参数：** 无

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "question": "帮我总结我的大一生活",
      "sort": 1,
      "status": 1,
      "statusLabel": "启用"
    },
    {
      "id": 4,
      "question": "如何填写个人档案信息？",
      "sort": 9,
      "status": 0,
      "statusLabel": "禁用"
    }
  ],
  "trace_id": "req-20260704-143025-abc123",
  "timestamp": "2026-07-04T14:30:25+08:00"
}
```

**响应字段：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 主键 |
| `question` | String | 问题内容 |
| `sort` | Integer | 排序号 |
| `status` | Integer | 0=禁用 1=启用 |
| `statusLabel` | String | 状态中文标签 |

> 返回**全部未删除记录（含禁用）**，按 `sort` 升序。

### 3.2 创建常见问题

**接口地址：** `POST /admin/ai-faqs`

**请求参数：**

| 参数名 | 类型 | 必填 | 校验 |
| :--- | :--- | :--- | :--- |
| `question` | String | 是 | `@NotBlank`，`@Size(max=200)` |
| `sort` | Integer | 是 | `@NotNull` |

**请求示例：**

```json
{
  "question": "我下学期应该多往哪方面发展",
  "sort": 3
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "创建成功",
  "data": {
    "id": 3,
    "question": "我下学期应该多往哪方面发展",
    "sort": 3,
    "status": 1,
    "statusLabel": "启用"
  },
  "trace_id": "req-20260704-143025-abc123",
  "timestamp": "2026-07-04T14:30:25+08:00"
}
```

**业务规则：**

- 固定以 `status=1` 创建（如需创建即禁用，创建后再调 3.3）；
- `question` 不做唯一性校验；
- 审计日志：`module=ai_faq`、`action=create`、`description=创建AI常见问题: #request.question`。

### 3.3 更新常见问题

**接口地址：** `PUT /admin/ai-faqs/{id}`

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 是 | 常见问题 ID |

**请求参数（均可选，局部更新）：**

| 参数名 | 类型 | 必填 | 校验 |
| :--- | :--- | :--- | :--- |
| `question` | String | 否 | `@Size(max=200)` |
| `sort` | Integer | 否 | — |
| `status` | Integer | 否 | `@Min(0)`、`@Max(1)` |

**请求示例：**

```json
{
  "question": "帮我复盘一下大一这一年的成长",
  "sort": 1,
  "status": 1
}
```

**响应示例：** 结构同 3.2 单条 `data`，`message` 为“更新成功”。

**业务规则：**

- 仅更新非 `null` 字段，未出现的字段保持不变；
- `status=0` 后学生端立即不返回该问题，管理端列表仍可见；
- 记录不存在或已软删除返回 `30001`；
- 审计日志：`module=ai_faq`、`action=update`。

### 3.4 删除常见问题

**接口地址：** `DELETE /admin/ai-faqs/{id}`

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 是 | 常见问题 ID |

**响应示例：**

```json
{
  "code": 0,
  "message": "删除成功",
  "data": null,
  "trace_id": "req-20260704-143025-abc123",
  "timestamp": "2026-07-04T14:30:25+08:00"
}
```

**业务规则：**

- 软删除：`UPDATE ai_faq_questions SET deleted_at = NOW() WHERE id = ? AND deleted_at IS NULL`；
- 记录不存在或已删除返回 `30001`；
- 不影响历史会话中已发送的消息内容；
- 审计日志：`module=ai_faq`、`action=delete`。

------

## 四、学生端接口

### 4.1 获取常见问题（FAQ）

**接口地址：** `GET /ai/faqs`

**请求参数：** 无

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": 1, "question": "帮我总结我的大一生活", "sort": 1 },
    { "id": 2, "question": "我未来适合做什么职业", "sort": 2 },
    { "id": 3, "question": "我下学期应该多往哪方面发展", "sort": 3 }
  ],
  "trace_id": "req-20260704-143025-abc123",
  "timestamp": "2026-07-04T14:30:25+08:00"
}
```

**响应字段：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 主键 |
| `question` | String | 问题内容（点击后作为消息内容） |
| `sort` | Integer | 排序号 |

> 仅返回 `status=1` 且 `deleted_at IS NULL` 的记录，按 `sort` 升序；响应不含 `status`/`statusLabel` 字段。
> **空列表约定：** 管理员禁用/删除全部问题后，`data` 返回 `[]`，前端应隐藏“常见问题”区块（或回退本地兜底文案）。

------

## 五、前端接入说明

### 5.1 渲染快捷问题

页面加载（进入 AI 助手页）时请求 `GET /ai/faqs`，将 `data` 渲染为快捷按钮；建议最多展示 6 条，超出部分折叠。

### 5.2 点击问题后的发送流程

```
1) 若当前没有会话：POST /ai/conversations  → 取 data.conversationId
2) 将该条 question 原样填入输入框并立即发送：
   POST /ai/conversations/{conversationId}/messages/stream   (推荐，9.4.1)
   或 POST /ai/conversations/{conversationId}/messages        (9.4，兜底)
   body: { "content": "帮我总结我的大一生活" }
3) 正常渲染流式响应；发送失败按既有消息重发逻辑处理
```

**注意：**

- 问题文本即消息内容，服务端**不做** `faqId` 之类的额外参数，前端不要自造字段；
- 发送成功后无需再标记 FAQ 使用次数（本模块不统计点击量）；
- 请求 `GET /ai/faqs` 失败时不应阻塞聊天主流程，可静默隐藏快捷区。

### 5.3 管理端页面建议

- 列表支持拖拽或输入 `sort` 排序，提供“启用/禁用”开关（调 3.3）、编辑弹窗（调 3.3）、删除二次确认（调 3.4）；
- 前端按钮文案直接使用 `question`，建议在管理端限制 200 字以内并提示“问题应面向学生成长场景，如‘帮我总结我的大一生活’”。

------

## 六、错误码

| Code | HTTP | 场景 |
| :--- | :--- | :--- |
| `0` | 200 | 成功 |
| `10001` | 400 | 参数错误：`question` 超 200 字符、`status` 非 0/1 |
| `10002` | 400 | 必填参数缺失：创建时 `question`/`sort` 为空 |
| `20005` | 403 | 非管理员且无 `ai:faq:manage` 权限码 |
| `30001` | 404 | 常见问题不存在或已软删除 |

------

## 七、测试用例

| 编号 | 接口 | 场景 | 期望 |
| :--- | :--- | :--- | :--- |
| FAQ-001 | `GET /admin/ai-faqs` | 管理员查询 | 返回全部（含禁用），`sort` 升序 |
| FAQ-002 | `POST /admin/ai-faqs` | 合法创建 | 成功，`status=1` |
| FAQ-003 | `POST /admin/ai-faqs` | `question` 缺失 / 超长 | `10002` / `10001` |
| FAQ-004 | `PUT /admin/ai-faqs/{id}` | 仅传 `sort` | 其他字段不变 |
| FAQ-005 | `PUT /admin/ai-faqs/{id}` | `status=0` | 学生端不再返回 |
| FAQ-006 | `PUT /admin/ai-faqs/{id}` | `status=2` | `10001` |
| FAQ-007 | `DELETE /admin/ai-faqs/{id}` | 删除 | 软删除，两端均不可见 |
| FAQ-008 | `PUT/DELETE /admin/ai-faqs/{id}` | ID 不存在 | `30001` |
| FAQ-009 | 全部管理端接口 | 非管理员且无权限码 | `20005` |
| FAQ-010 | `GET /ai/faqs` | 全部禁用 | `data: []` |
| FAQ-011 | `GET /ai/faqs` | 未登录 | `20001` |
| FAQ-012 | 点击问题 | 无会话时点击 | 自动创建会话并成功发送，回复正常流式返回 |

------

## 八、变更记录

| 日期 | 版本 | 内容 |
| :--- | :--- | :--- |
| 2026-11-23 | V1.0 | 新增 AI 常见问题配置模块：`ai_faq_questions` 表、`/admin/ai-faqs` CRUD、`GET /ai/faqs`、权限码 `ai:faq:manage`（Flyway V59） |
