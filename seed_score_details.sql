-- ============================================================
-- 种子数据：评分计算明细（score_calculation_details）
--
-- 用途：供「获取分数计算说明」接口（GET /profile/scores/{calculationId}/details）
--       返回各指标的 raw_score / weight / weighted_score / source_archive_ids。
--
-- 说明：仅覆盖当前学期（semester_id=4）的 5 名种子学生（张三~陈七），
--       每个学生取 5~8 条其真实档案对应的指标，与 seed_dashboard.sql 中
--       该学生的档案记录一一对应（source_archive_ids = archives.id）。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql      （schools/colleges/majors/classes/users）
--   2. seed_semesters.sql     （semesters 1~5）
--   3. seed_roles_permissions.sql
--   4. seed_dictionaries.sql
--   5. seed_indicators.sql    （ability_dimensions / indicator_rule_versions / evaluation_indicators）
--   6. seed_dashboard.sql     （score_calculations id=1~5 / archives）
--
-- 口径（与《学生端接口文档》4.1.3 一致）：
--   - indicatorName      → evaluation_indicators（level-3 指标）
--   - dimensionName      → ability_dimensions.dimension_name
--   - weight             → evaluation_indicators.weight
--   - weightedScore      → raw_score × weight
--   - sourceArchiveTitles→ archives（source_archive_ids 对应当前用户的档案）
-- ============================================================

INSERT INTO `score_calculation_details`
(`id`, `calculation_id`, `indicator_id`, `dimension_code`, `raw_score`, `weight`, `weighted_score`, `source_archive_ids`) VALUES
-- ---- 张三（user_id=1，calculation_id=1）----
(1, 1, 16, 'academic',      92.00, 0.1200, 11.04, NULL),
(2, 1, 18, 'academic',     100.00, 0.0800,  8.00, '[7]'),   -- 大学英语六级证书
(3, 1, 20, 'competition',   90.00, 0.0900,  8.10, '[11]'),  -- ICPC 亚洲区预选赛
(4, 1, 21, 'competition',   95.00, 0.0600,  5.70, '[1]'),   -- 数学建模省级一等奖
(5, 1, 24, 'research',      85.00, 0.0800,  6.80, '[4]'),   -- 大创项目立项
(6, 1, 28, 'socialWork',    80.00, 0.0600,  4.80, '[8]'),   -- 院学生会宣传部干事
(7, 1, 30, 'socialWork',   100.00, 0.0400,  4.00, '[3]'),   -- 三下乡社会实践
(8, 1, 32, 'comprehensive', 85.00, 0.0400,  3.40, '[2]'),   -- 国家励志奖学金
-- ---- 李四（user_id=2，calculation_id=2）----
(9,  2, 16, 'academic',      85.00, 0.1200, 10.20, NULL),
(10, 2, 21, 'competition',   80.00, 0.0600,  4.80, '[13]'), -- 全国大学生英语竞赛（校级三等奖）
(11, 2, 24, 'research',      70.00, 0.0800,  5.60, '[17]'), -- 学术论文（合作作者）
(12, 2, 26, 'research',      75.00, 0.0500,  3.75, '[17]'), -- 学术论文（合作作者）
(13, 2, 30, 'socialWork',   100.00, 0.0400,  4.00, '[15]'), -- 养老院志愿服务活动
(14, 2, 32, 'comprehensive', 90.00, 0.0400,  3.60, '[14]'), -- 国家奖学金
-- ---- 王五（user_id=3，calculation_id=3）----
(15, 3, 16, 'academic',      95.00, 0.1200, 11.40, NULL),
(16, 3, 20, 'competition',   90.00, 0.0900,  8.10, '[29]'), -- 中国国际大学生创新大赛
(17, 3, 21, 'competition',   95.00, 0.0600,  5.70, '[21]'), -- 挑战杯省级一等奖
(18, 3, 24, 'research',      95.00, 0.0800,  7.60, '[23]'), -- 国家级大创立项
(19, 3, 26, 'research',     100.00, 0.0500,  5.00, '[24]'), -- 发表 SCI 论文
(20, 3, 28, 'socialWork',   100.00, 0.0600,  6.00, '[27]'), -- 校学生会主席团成员
(21, 3, 30, 'socialWork',   100.00, 0.0400,  4.00, '[25]'), -- 乡村振兴调研社会实践
(22, 3, 32, 'comprehensive', 90.00, 0.0400,  3.60, '[22]'), -- 校级一等奖学金
-- ---- 赵六（user_id=4，calculation_id=4）----
(23, 4, 16, 'academic',      88.00, 0.1200, 10.56, NULL),
(24, 4, 21, 'competition',   80.00, 0.0600,  4.80, '[30]'), -- 蓝桥杯省级三等奖
(25, 4, 24, 'research',      85.00, 0.0800,  6.80, '[35]'), -- 参与教师横向课题
(26, 4, 29, 'socialWork',    95.00, 0.0300,  2.85, '[32]'), -- 博物馆志愿讲解
(27, 4, 30, 'socialWork',   100.00, 0.0400,  4.00, '[32]'), -- 博物馆志愿讲解
(28, 4, 32, 'comprehensive', 75.00, 0.0400,  3.00, '[31]'), -- 校级二等奖学金
-- ---- 陈七（user_id=5，calculation_id=5）----
(29, 5, 16, 'academic',      80.00, 0.1200,  9.60, NULL),
(30, 5, 21, 'competition',   75.00, 0.0600,  4.50, '[36]'), -- 大学生电子设计竞赛（校级二等奖）
(31, 5, 24, 'research',      60.00, 0.0800,  4.80, '[40]'), -- 参与大创项目
(32, 5, 29, 'socialWork',    85.00, 0.0300,  2.55, '[38]'), -- 社区环保公益活动
(33, 5, 30, 'socialWork',   100.00, 0.0400,  4.00, '[38]'), -- 社区环保公益活动
(34, 5, 32, 'comprehensive', 80.00, 0.0400,  3.20, '[37]'); -- 国家励志奖学金
