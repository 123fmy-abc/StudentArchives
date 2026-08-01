-- ============================================================
-- 种子数据：评价指标树（ability_dimensions + indicator_rule_versions + evaluation_indicators）
--
-- 用途：供 `GET /common/indicators` 指标树查询接口测试。
--      不传 versionId 时返回当前生效版本（effective_at <= NOW() 且最新的一条）。
--
-- 前提：已执行 seed_students.sql（schools 存在 id=1，users 存在 id=1~5），
--      指标树 school_id=1 对应华中科技大学。
--
-- 对应架构：
--   - ability_dimensions       系统级能力维度字典（无 school_id，口径统一）
--   - indicator_rule_versions  学校级指标规则快照版本（全局版本号）
--   - evaluation_indicators    学校级指标树（parent_id 自关联，一/二/三级）
--   - indicator_versions       单指标历史版本（由应用层在发布时维护，本种子不直接插入）
--
-- 权重约定：同一父节点下所有子节点权重之和 = 父节点权重；一级节点权重之和 = 1。
-- 指标编码（indicator_code）：学校内唯一（UNIQUE(school_id, indicator_code)）。
-- 三级指标必填 scoring_rule（计分规则 JSON），type 取值：AVG/SUM/MAX/WEIGHTED/THRESHOLD/COUNT。
-- ============================================================

-- ============================================================
-- 1. 能力维度字典（ability_dimensions）
--    维度编码与首页概览 / 学生端接口文档中的 dimensionCode 保持一致
-- ============================================================
INSERT INTO `ability_dimensions` (`id`, `dimension_name`, `dimension_code`, `description`, `sort`, `status`) VALUES
(1, '学业成绩',   'academic',      '必修/选修课程成绩、绩点、外语水平等学业相关表现', 1, 1),
(2, '竞赛实践',   'competition',   '学科竞赛、创新创业竞赛等获奖与实践经历',           2, 1),
(3, '科研创新',   'research',      '科研项目、论文、知识产权等科研创新成果',           3, 1),
(4, '社会工作',   'socialWork',    '学生组织任职、社会公益、志愿服务等社会参与',       4, 1),
(5, '综合素质',   'comprehensive', '思想品德、荣誉称号、身心素质等综合素养',           5, 1);

-- ============================================================
-- 2. 指标规则快照版本（indicator_rule_versions）
--    version=1 为当前生效版本，effective_at 必须 <= NOW()
--    （findCurrentEffective 按 effective_at <= NOW() 倒序取最新一条）
-- ============================================================
INSERT INTO `indicator_rule_versions` (`id`, `school_id`, `version`, `version_name`, `effective_at`, `created_by`) VALUES
(1, 1, 1, '2025-2026学年 第一版', '2026-02-23 08:00:00', 1);

-- ============================================================
-- 3. 评价指标树（evaluation_indicators）
--    共 35 条：一级 5 条 + 二级 10 条 + 三级 20 条
--    三级指标携带 scoring_rule 计分规则；所有指标 version=1 关联上面的规则版本。
--
--    指标树结构：
--      ├─ 001 学业成绩(academic)      ─ 001.001 学业课程成绩  ─ 001.001.001 必修课平均绩点
--      │                              └ 001.002 外语水平       └ 001.001.002 必修课优良率
--      │                                                       └ 001.002.001 英语等级证书
--      │                                                       └ 001.002.002 第二外语
--      ├─ 002 竞赛实践(competition)   ─ 002.001 学科竞赛        ─ 002.001.001 国家级学科竞赛
--      │                              └ 002.002 创新创业竞赛    └ 002.001.002 省级学科竞赛
--      │                                                       └ 002.002.001 创新创业获奖
--      │                                                       └ 002.002.002 创新创业项目落地
--      ├─ 003 科研创新(research)      ─ 003.001 科研项目        ─ 003.001.001 主持/参与科研项目
--      │                              └ 003.002 学术成果        └ 003.001.002 科研项目结题
--      │                                                       └ 003.002.001 发表论文
--      │                                                       └ 003.002.002 知识产权
--      ├─ 004 社会工作(socialWork)    ─ 004.001 社会工作        ─ 004.001.001 学生组织任职
--      │                              └ 004.002 志愿服务        └ 004.001.002 社会公益活动
--      │                                                       └ 004.002.001 志愿时长
--      │                                                       └ 004.002.002 志愿服务评价
--      └─ 005 综合素质(comprehensive) ─ 005.001 思想品德        ─ 005.001.001 荣誉称号
--                                     └ 005.002 身心素质        └ 005.001.002 遵纪守法
--                                                                └ 005.002.001 体质测试
--                                                                └ 005.002.002 心理健康
-- ============================================================

-- ---------- 一级指标（level=1，parent_id=NULL，权重之和=1） ----------
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`, `weight`, `scoring_rule`, `dimension_code`, `status`, `version`, `sort`) VALUES
(1, 1, '学业成绩', 'ACAD_OVERALL', NULL, 1, '001', 0.3000, NULL, 'academic',      1, 1, 1),
(2, 1, '竞赛实践', 'COMP_OVERALL', NULL, 1, '002', 0.2500, NULL, 'competition',   1, 1, 2),
(3, 1, '科研创新', 'RES_OVERALL',  NULL, 1, '003', 0.2000, NULL, 'research',      1, 1, 3),
(4, 1, '社会工作', 'SOC_OVERALL',  NULL, 1, '004', 0.1500, NULL, 'socialWork',    1, 1, 4),
(5, 1, '综合素质', 'COM_OVERALL',  NULL, 1, '005', 0.1000, NULL, 'comprehensive', 1, 1, 5);

-- ---------- 二级指标（level=2，子权重之和=父权重） ----------
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`, `weight`, `scoring_rule`, `dimension_code`, `status`, `version`, `sort`) VALUES
(6,  1, '学业课程成绩', 'ACAD_COURSE',       1, 2, '001.001', 0.1800, NULL, 'academic',      1, 1, 1),
(7,  1, '外语水平',     'ACAD_LANG',         1, 2, '001.002', 0.1200, NULL, 'academic',      1, 1, 2),
(8,  1, '学科竞赛',     'COMP_DISCIPLINE',   2, 2, '002.001', 0.1500, NULL, 'competition',   1, 1, 1),
(9,  1, '创新创业竞赛', 'COMP_INNOVATION',   2, 2, '002.002', 0.1000, NULL, 'competition',   1, 1, 2),
(10, 1, '科研项目',     'RES_PROJECT',       3, 2, '003.001', 0.1200, NULL, 'research',      1, 1, 1),
(11, 1, '学术成果',     'RES_OUTCOME',       3, 2, '003.002', 0.0800, NULL, 'research',      1, 1, 2),
(12, 1, '社会工作',     'SOC_WORK',          4, 2, '004.001', 0.0900, NULL, 'socialWork',    1, 1, 1),
(13, 1, '志愿服务',     'SOC_VOLUNTEER',     4, 2, '004.002', 0.0600, NULL, 'socialWork',    1, 1, 2),
(14, 1, '思想品德',     'COM_MORAL',         5, 2, '005.001', 0.0600, NULL, 'comprehensive', 1, 1, 1),
(15, 1, '身心素质',     'COM_BODY',          5, 2, '005.002', 0.0400, NULL, 'comprehensive', 1, 1, 2);

-- ---------- 三级指标（level=3，scoring_rule 必填） ----------
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`, `weight`, `scoring_rule`, `dimension_code`, `status`, `version`, `sort`) VALUES
(16, 1, '必修课程平均绩点', 'ACAD_GPA',        6,  3, '001.001.001', 0.1200, '{"type":"AVG","source":"required_course_scores"}',                'academic',      1, 1, 1),
(17, 1, '必修课程优良率',   'ACAD_EXCELLENT',  6,  3, '001.001.002', 0.0600, '{"type":"THRESHOLD","source":"course_excellent_rate","threshold":60,"score":100}', 'academic',      1, 1, 2),
(18, 1, '英语等级证书',     'ACAD_ENGLISH',    7,  3, '001.002.001', 0.0800, '{"type":"MAX","source":"certificate_level"}',                      'academic',      1, 1, 1),
(19, 1, '第二外语',         'ACAD_OTHER_LANG', 7,  3, '001.002.002', 0.0400, '{"type":"COUNT","source":"language_certificate_count","perUnit":1,"scorePerUnit":20}', 'academic', 1, 1, 2),
(20, 1, '国家级学科竞赛',   'COMP_NATIONAL',   8,  3, '002.001.001', 0.0900, '{"type":"SUM","source":"competition_scores","max":100}',          'competition',   1, 1, 1),
(21, 1, '省级学科竞赛',     'COMP_PROVINCIAL', 8,  3, '002.001.002', 0.0600, '{"type":"SUM","source":"competition_scores","max":100}',          'competition',   1, 1, 2),
(22, 1, '创新创业获奖',     'COMP_INNO_AWARD', 9,  3, '002.002.001', 0.0700, '{"type":"COUNT","source":"award_count","perUnit":1,"scorePerUnit":10}', 'competition', 1, 1, 1),
(23, 1, '创新创业项目落地', 'COMP_INNO_PROJECT', 9, 3, '002.002.002', 0.0300, '{"type":"COUNT","source":"innovation_project_count","perUnit":1,"scorePerUnit":20}', 'competition', 1, 1, 2),
(24, 1, '主持/参与科研项目', 'RES_PROJ_JOIN', 10, 3, '003.001.001', 0.0800, '{"type":"COUNT","source":"research_project_count","perUnit":1,"scorePerUnit":10}', 'research', 1, 1, 1),
(25, 1, '科研项目结题',     'RES_PROJ_DONE',  10,  3, '003.001.002', 0.0400, '{"type":"THRESHOLD","source":"research_completion_rate","threshold":100,"score":100}', 'research', 1, 1, 2),
(26, 1, '发表论文',         'RES_PAPER',      11,  3, '003.002.001', 0.0500, '{"type":"COUNT","source":"paper_count","perUnit":1,"scorePerUnit":25}', 'research', 1, 1, 1),
(27, 1, '知识产权',         'RES_PATENT',     11,  3, '003.002.002', 0.0300, '{"type":"COUNT","source":"patent_count","perUnit":1,"scorePerUnit":30}', 'research', 1, 1, 2),
(28, 1, '学生组织任职',     'SOC_POSITION',   12,  3, '004.001.001', 0.0600, '{"type":"MAX","source":"org_position_level"}',                     'socialWork',    1, 1, 1),
(29, 1, '社会公益活动',     'SOC_PUBLIC',     12,  3, '004.001.002', 0.0300, '{"type":"COUNT","source":"public_welfare_count","perUnit":1,"scorePerUnit":10}', 'socialWork', 1, 1, 2),
(30, 1, '志愿时长',         'SOC_VOL_HOURS',  13,  3, '004.002.001', 0.0400, '{"type":"THRESHOLD","source":"volunteer_hours","threshold":40,"score":100}', 'socialWork', 1, 1, 1),
(31, 1, '志愿服务评价',     'SOC_VOL_RATING', 13,  3, '004.002.002', 0.0200, '{"type":"MAX","source":"volunteer_evaluation"}',                   'socialWork',    1, 1, 2),
(32, 1, '荣誉称号',         'COM_HONOR',      14,  3, '005.001.001', 0.0400, '{"type":"MAX","source":"honor_level"}',                            'comprehensive', 1, 1, 1),
(33, 1, '遵纪守法',         'COM_DISCIPLINE', 14,  3, '005.001.002', 0.0200, '{"type":"THRESHOLD","source":"violation_count","threshold":0,"score":100}', 'comprehensive', 1, 1, 2),
(34, 1, '体质测试',         'COM_SPORTS',     15,  3, '005.002.001', 0.0250, '{"type":"THRESHOLD","source":"physical_test_score","threshold":60,"score":100}', 'comprehensive', 1, 1, 1),
(35, 1, '心理健康',         'COM_PSYCH',      15,  3, '005.002.002', 0.0150, '{"type":"MAX","source":"mental_health_level"}',                    'comprehensive', 1, 1, 2);
