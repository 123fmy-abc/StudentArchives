# 成长时间轴计分规则表（方案 A）实施计划

## Context

当前多维能力画像的分数由学生在新增成长时间轴事件时自填（`growth_timeline_abilities.score`，0-100/维度/事件），存在无计分标准、横向不可比、无审核把关的问题。本次改造为**方案 A：管理端计分规则表**：

- 管理员按**事件类型**（1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升）配置规则：该类事件对各能力维度的默认分值
- 学生新增/编辑事件时**不再填分数**，维度和分数由后端按规则自动生成
- 无规则的事件类型：事件正常创建，但不产生能力得分行（不报错）
- **存量自填分数保留不动**；仅新事件及"修改了事件类型"的编辑按规则计分
- 规则全局生效（与 `ability_dimensions` 一致，无 school_id）

## 改动清单

### 1. Flyway 迁移（新建）
`src/main/resources/db/migration/V51__growth_event_score_rules.sql`

- 表 `growth_event_score_rules`：`id`、`event_type` INT NOT NULL（UNIQUE 条件索引 `uk_gesr_event_type(event_type, is_deleted_null)`）、`abilities` JSON NOT NULL（元素 `[{dimensionCode, score}]`）、`remark` VARCHAR(255) NULL、`created_at`/`updated_at`/`deleted_at`/`is_deleted_null` 生成列（照抄 V7/V9 惯例）
- 不做种子数据（维度编码由各校自行配置，规则由管理员在界面上配置）

### 2. Entity + Repository（新建）
- `entity/foundation/GrowthEventScoreRule.java`：继承 BaseEntity，`@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)`（注意：不能放基类，见 BaseEntity L14-22 注释）；`eventType`、`abilities`（String + `columnDefinition = "JSON"`，参照 EvaluationIndicator 的 JSON 列写法）、`remark`
- `repository/GrowthEventScoreRuleRepository.java`：`findByEventType(Integer)`、`findAllByOrderByEventTypeAsc()`

### 3. DTO（新建，包 `dto/Fmy/growthscorerule/`）
- `GrowthScoreRuleUpsertRequest`：`eventType`（@NotNull @Min(1) @Max(6)）、`abilities`（@NotEmpty @Valid List）、`remark`
- `GrowthScoreRuleItem`：`dimensionCode`（@NotBlank @Size(max=50)）、`score`（@DecimalMin("0") @DecimalMax("100")）
- `GrowthScoreRuleResponse`：id、eventType、eventTypeLabel、abilities、remark
- Service 层业务校验：eventType 唯一（重复建返回 DATA_DUPLICATE）、abilities 内 dimensionCode 不重复、dimensionCode 必须存在于 `ability_dimensions` 且启用（复用 `AbilityDimensionRepository`）

### 4. 管理端 Service + Controller（新建，照抄 AdminAbilityDimension 模式）
- `service/Fmy/AdminGrowthEventScoreRuleService.java`：list / create / update / delete（软删除），每方法 `adminAuthService.requireAdminOrPermission(userId, "indicator:manage")`（复用能力维度同一权限码，无需迁移权限种子）
- `controller/Fmy/AdminGrowthEventScoreRuleController.java`：`/admin/growth-event-score-rules`，GET / POST / PUT `/{id}` / DELETE `/{id}`，写操作加 `@AuditLog(module = "growth_event_score_rule", ...)`

### 5. 学生端打分逻辑改造（修改）
`service/Fmy/ProfileGrowthTimelineService.java`：

- 新增私有方法 `saveAbilitiesFromRule(Long timelineId, Integer eventType)`：查规则（`growthEventScoreRuleRepository.findByEventType`），无规则直接 return；有则用 Jackson 把 `abilities` JSON 反序列化为 `[{dimensionCode, score}]` 并 `saveAll`
- `create()` L162：`saveAbilities(timeline.getId(), request.getAbilityData())` → `saveAbilitiesFromRule(timeline.getId(), request.getEventType())`
- `update()` L268-271：删除原 `if (request.getAbilityData() != null)` 分支；改为"当传入的 `eventType` 与库里实际值**不同**时"，`softDeleteAbilities(id)` + `saveAbilitiesFromRule(id, 新eventType)`——保证存量事件在只改名称/描述等字段时分数原样保留
- `GrowthTimelineCreateRequest.abilityData` / `GrowthTimelineUpdateRequest.abilityData` 字段**保留但忽略**（向后兼容旧客户端），字段注释标明"已废弃，后端忽略"
- `GrowthTimelineAbilityItem` 保留（管理端响应/详情响应仍复用该结构）
- 详情响应 `toDetail` 不变

### 6. 文档更新
- `docs/学生端接口文档.md` 4.2：标注 `abilityData` 已废弃（传入即忽略），得分由管理端规则生成，无规则时事件无能力记录
- `docs/管理端接口文档.md`：新增"成长事件计分规则"模块（4 个端点、示例、event_type 枚举、权限码 indicator:manage）

## 验证

1. `mvn test`（或 `mvn compile`）确认编译与迁移加载通过
2. `mvn spring-boot:run` 后手工验证：
   - 管理员建规则（event_type=1，2 个维度项）→ 重复建同类型规则返回 DATA_DUPLICATE；填不存在的 dimensionCode / score>100 返回参数错误
   - 学生创建事件（故意带 abilityData）→ 详情中能力分为规则值，自填被忽略
   - 学生编辑该事件仅改名称 → 能力分不变；改为其他 event_type → 能力分按新类型规则重建
   - 删除某类型规则 → 创建该类型事件成功且无能力行，`GET /archive/dimensions` 正常（该维度计 0 分）
