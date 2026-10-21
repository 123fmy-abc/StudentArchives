-- ============================================================
-- 种子数据：数据字典（dictionaries 表）
--
-- 对应架构中的"字典表"策略（区别于代码级 Enum）：
--
-- 【字典表】纯分类/展示型数据，用 string dict_code 存储在业务表字段中，
--           无业务逻辑/状态机，后端通过 /common/dict 提供查询。
--           管理端可维护，无需改代码发布。
--
-- 【代码级 Enum】int 值存储在 DB 或有状态机逻辑的枚举：
--   - GenderEnum (gender)          — DB 存 int（0/1/2），通过 /common/enums 查询
--   - ApplyStatusEnum (apply_status) — DB 存 int，有状态机流转规则
--   - ScopeTypeEnum (scope_type)   — 权限范围分类
--   - AuditActionEnum (audit_action) — 审核操作类型
--   - EventTypeEnum (event_type)   — 时间轴事件类型
--   - RoleLevelEnum (role_level)   — 角色级别
--   - ArchiveTypeEnum              — 档案类型，由 archive_type_configs 表独立管理
--
-- 【分类矩阵】
--   dict_type             | DB 存储方式 | 查询接口               | 有无业务逻辑 | 归属
--   ----------------------|-------------|------------------------|-------------|--------
--   competition_level     | string code | /common/dict           | 无           | 字典表 ✓
--   award_level           | string code | /common/dict           | 无           | 字典表 ✓
--   scholarship_level     | string code | /common/dict           | 无           | 字典表 ✓
--   scholarship_award_lev | string code | /common/dict           | 无           | 字典表 ✓
--   company_type          | string code | /common/dict           | 无           | 字典表 ✓
--   industry_type         | string code | /common/dict           | 无           | 字典表 ✓
--   research_type         | string code | /common/dict           | 无           | 字典表 ✓
--   project_level         | string code | /common/dict           | 无           | 字典表 ✓
--   organization_level    | string code | /common/dict           | 无           | 字典表 ✓
--   message_category      | string code | /common/dict           | 无           | 字典表 ✓
--   political_status      | string code | /common/dict           | 无           | 字典表 ✓
--   competition_type      | string code | /common/dict           | 无           | 字典表 ✓
--   gender                | int (0/1/2) | /common/enums          | 展示标签     | 代码 Enum ✓
--   apply_status          | int (0~4)   | /common/enums          | 状态机       | 代码 Enum ✓
--   scope_type            | int         | /common/enums          | 权限计算     | 代码 Enum ✓
--   audit_action          | int         | /common/enums          | 审核逻辑     | 代码 Enum ✓
--   event_type            | int         | /common/enums          | 时间轴聚合   | 代码 Enum ✓
--   role_level            | int         | /common/enums          | 权限层级     | 代码 Enum ✓
--   archive_category      | string code | /common/dict           | 独立配置     | 独立表 ✓ (archive_type_configs)
--
-- 注意：
-- 1. 不要将 gender 和 apply_status 加入 dictionaries 表
--    — 它们的 DB 字段存的是 int，与 dict_code 的 string 类型不匹配，
--      前端应使用 /common/enums 获取 int→label 映射。
-- 2. archive_category 由 archive_type_configs 表独立管理，此处不维护。
-- ============================================================

-- ============================================================
-- 1. 竞赛级别（competition_level）
--    字段引用举例：award_competition_stars.competition_level
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(1,  'competition_level', 'national',    '国家级', 1, 1),
(2,  'competition_level', 'provincial',  '省部级', 2, 1),
(3,  'competition_level', 'school',      '校级',   3, 1),
(4,  'competition_level', 'college',     '院级',   4, 1);

-- ============================================================
-- 2. 获奖等级（award_level）
--    字段引用举例：archive_competitions.award_level
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(5,  'award_level', 'special',    '特等奖', 1, 1),
(6,  'award_level', 'first',      '一等奖', 2, 1),
(7,  'award_level', 'second',     '二等奖', 3, 1),
(8,  'award_level', 'third',      '三等奖', 4, 1),
(9,  'award_level', 'excellence', '优秀奖', 5, 1);

-- ============================================================
-- 3. 奖学金级别（scholarship_level）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(10, 'scholarship_level', 'national',   '国家奖学金',   1, 1),
(11, 'scholarship_level', 'provincial', '省级奖学金',   2, 1),
(12, 'scholarship_level', 'school',     '校级奖学金',   3, 1),
(13, 'scholarship_level', 'enterprise', '企业奖学金',   4, 1);

-- ============================================================
-- 4. 奖学金获奖等级（scholarship_award_level）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(14, 'scholarship_award_level', 'special', '特等奖', 1, 1),
(15, 'scholarship_award_level', 'first',   '一等奖', 2, 1),
(16, 'scholarship_award_level', 'second',  '二等奖', 3, 1),
(17, 'scholarship_award_level', 'third',   '三等奖', 4, 1);

-- ============================================================
-- 5. 公司类型（company_type，创新创业业务使用）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(18, 'company_type', 'limited_liability',  '有限责任公司', 1, 1),
(19, 'company_type', 'joint_stock',        '股份有限公司', 2, 1),
(20, 'company_type', 'partnership',        '合伙企业',     3, 1),
(21, 'company_type', 'sole_proprietorship', '个体工商户',  4, 1),
(22, 'company_type', 'other',              '其他',         5, 1);

-- ============================================================
-- 6. 行业类型（industry_type，创新创业/实习业务使用）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(23, 'industry_type', 'internet',      '互联网/信息技术',  1, 1),
(24, 'industry_type', 'finance',       '金融',             2, 1),
(25, 'industry_type', 'education',     '教育/培训',        3, 1),
(26, 'industry_type', 'manufacturing', '制造业',           4, 1),
(27, 'industry_type', 'medical',       '医疗健康',         5, 1),
(28, 'industry_type', 'real_estate',   '房地产/建筑',      6, 1),
(29, 'industry_type', 'energy',        '能源/环保',        7, 1),
(30, 'industry_type', 'agriculture',   '农林牧渔',         8, 1),
(31, 'industry_type', 'culture',       '文化传媒',         9, 1),
(32, 'industry_type', 'consulting',    '咨询/商务服务',   10, 1),
(33, 'industry_type', 'other',         '其他',            11, 1);

-- ============================================================
-- 7. 研究类型（research_type，学术研究业务使用）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(34, 'research_type', 'basic',       '基础研究', 1, 1),
(35, 'research_type', 'applied',     '应用研究', 2, 1),
(36, 'research_type', 'experimental', '实验发展', 3, 1),
(37, 'research_type', 'other',       '其他',     4, 1);

-- ============================================================
-- 8. 项目级别（project_level）
--    字段引用举例：archive_research.project_level
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(38, 'project_level', 'national',   '国家级', 1, 1),
(39, 'project_level', 'provincial', '省部级', 2, 1),
(40, 'project_level', 'school',     '校级',   3, 1),
(41, 'project_level', 'college',    '院级',   4, 1);

-- ============================================================
-- 9. 组织级别（organization_level，组织履历业务使用）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(42, 'organization_level', 'school',     '校级', 1, 1),
(43, 'organization_level', 'college',    '院级', 2, 1),
(44, 'organization_level', 'department', '系级', 3, 1),
(45, 'organization_level', 'other',      '其他', 4, 1);

-- ============================================================
-- 10. 消息分类（message_category）
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(46, 'message_category', 'system_notice',   '系统通知', 1, 1),
(47, 'message_category', 'audit_remind',    '审批提醒', 2, 1),
(48, 'message_category', 'dynamic_remind',  '动态提醒', 3, 1),
(49, 'message_category', 'private_message', '私信',     4, 1);

-- ============================================================
-- 11. 政治面貌（political_status）
--     字段引用举例：student_profiles.political_status
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(50, 'political_status', 'party_member',     '中共党员',     1, 1),
(51, 'political_status', 'party_member_pre', '中共预备党员', 2, 1),
(52, 'political_status', 'league_member',    '共青团员',     3, 1),
(53, 'political_status', 'masses',           '群众',         4, 1),
(54, 'political_status', 'other_party',      '民主党派',     5, 1);

-- ============================================================
-- 12. 竞赛类型（competition_type）
--     字段引用举例：archive_competitions.competition_type
-- ============================================================
INSERT INTO `dictionaries` (`id`, `dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
(55, 'competition_type', 'academic',    '学科类',       1, 1),
(56, 'competition_type', 'technology',  '科技类',       2, 1),
(57, 'competition_type', 'innovation',  '创新创业类',   3, 1),
(58, 'competition_type', 'art',         '文艺类',       4, 1),
(59, 'competition_type', 'sports',      '体育类',       5, 1),
(60, 'competition_type', 'other',       '其他',         6, 1);
