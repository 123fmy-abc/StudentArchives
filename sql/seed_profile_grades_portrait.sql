-- ============================================================
-- 种子数据：档案概览「期末成绩 + 多维度画像」完整链路
-- 目标学生：郑凯（学号 202410104，users.id 由脚本动态解析）
--
-- 覆盖链路（与 ProfileService.buildProfileInfo 的取数口径一致）：
--   semesters（当前学期 / 上一学期，保证 is_current=1 有且仅有一条）
--   → ability_dimensions（能力维度字典 academic/competition/practice/quality/moral）
--   → evaluation_indicators（1/2/3 级指标，仅在该校「没有任何启用指标」时播种）
--   → indicator_rule_versions（规则版本，评分引擎 resolveRuleForSemester 依赖）
--   → courses / gpa_records（课程主数据与期末成绩）
--   → semester_gpa_summaries（学期成绩汇总，含班级/专业排名）
--   → score_calculations / score_calculation_details（评分批次与三级指标明细）
--   → portrait_evaluation_scores（各能力维度画像分，档案概览「多维度画像」数据源）
--
-- 关键口径（见 AdminScoreService.processStudent）：
--   维度得分 = Σ(三级指标 raw_score × 三级指标 weight)
--   维度目标分 = 一级指标 weight × 100
--   一批维度目标分之和 = 100（maxTotalScore 恒为 100）
--
-- 前置条件：已执行 sql/reset_seed_org_users.sql（存在学号 202410104 的郑凯）
-- 幂等性：
--   1. 成绩按唯一键 UPSERT，重复执行只会覆盖同一批演示成绩；
--   2. 评分批次以 data_source='种子数据链路' 标记，重跑时先删后插，不碰引擎真实产出；
--   3. 指标树只在该校「没有任何启用指标」时播种，绝不覆盖线上已配置的指标与权重。
-- 事务：整体单事务，末尾 COMMIT（放弃执行可把 COMMIT 换成 ROLLBACK）。
--
-- 执行：mysql -u<user> -p student_archives < sql/seed_profile_grades_portrait.sql
--       或在客户端中整段执行（会先 USE student_archives）。
-- ============================================================

USE `student_archives`;

START TRANSACTION;

-- ============================================================
-- 1. 参数解析：学校 / 学生 / 班级 / 专业 / 学院
-- ============================================================
SET @user_id  = (SELECT `id` FROM `users`
                 WHERE `user_no` = '202410104' AND `deleted_at` IS NULL
                 ORDER BY `id` LIMIT 1);
SET @school_id = (SELECT `school_id` FROM `users` WHERE `id` = @user_id);
SET @class_id  = (SELECT `class_id` FROM `student_profiles`
                  WHERE `user_id` = @user_id AND `deleted_at` IS NULL
                  ORDER BY `id` DESC LIMIT 1);
SET @major_id  = (SELECT `major_id`  FROM `classes` WHERE `id` = @class_id);
SET @college_id = (SELECT `college_id` FROM `majors` WHERE `id` = @major_id);

-- 前置校验：以下四个值任一为 NULL 说明种子学生不存在，请先执行 reset_seed_org_users.sql
SELECT @user_id AS `user_id`, @school_id AS `school_id`,
       @class_id AS `class_id`, @major_id AS `major_id`;

-- ============================================================
-- 2. 学期：确保存在「当前学期」（档案概览只取 is_current=1 的学期）
--    如需锁定其它学期，改这里的口径即可（例如按 name 指定）
-- ============================================================
SET @semester_id = (SELECT `id` FROM `semesters`
                    WHERE `school_id` = @school_id AND `is_current` = 1 AND `deleted_at` IS NULL
                    ORDER BY `id` DESC LIMIT 1);

-- 没有当前学期时退化为「最近开始的一个学期」
SET @semester_id = COALESCE(@semester_id,
    (SELECT `id` FROM `semesters`
     WHERE `school_id` = @school_id AND `deleted_at` IS NULL
     ORDER BY `start_date` DESC, `id` DESC LIMIT 1));

-- 该校一个学期都没有时新建 2026-2027-1
INSERT INTO `semesters` (`school_id`, `name`, `start_date`, `end_date`, `is_current`, `status`)
SELECT @school_id, '2026-2027-1', '2026-09-01', '2027-01-15', 1, 1
FROM DUAL WHERE @semester_id IS NULL;

SET @semester_id = COALESCE(@semester_id,
    (SELECT `id` FROM `semesters`
     WHERE `school_id` = @school_id AND `name` = '2026-2027-1' AND `deleted_at` IS NULL
     ORDER BY `id` DESC LIMIT 1));

-- 学期唯一当前化：ProfileService 用 is_current=1 AND status=1 定位当前学期，
-- 若出现多条 is_current=1 会抛 NonUniqueResultException，这里收敛成唯一一条
UPDATE `semesters` SET `is_current` = 0, `updated_at` = NOW()
WHERE `school_id` = @school_id AND `id` <> @semester_id
  AND `is_current` = 1 AND `deleted_at` IS NULL;

UPDATE `semesters` SET `is_current` = 1, `status` = 1, `updated_at` = NOW()
WHERE `id` = @semester_id AND (`is_current` <> 1 OR `status` <> 1);

-- 上一学期（按 start_date 取当前学期之前最近的一条），用于成绩趋势与画像 change
SET @prev_semester_id = (SELECT `id` FROM `semesters`
    WHERE `school_id` = @school_id AND `deleted_at` IS NULL
      AND `start_date` < (SELECT `start_date` FROM `semesters` WHERE `id` = @semester_id)
    ORDER BY `start_date` DESC, `id` DESC LIMIT 1);

-- ============================================================
-- 3. 清理上一次的种子评分数据（只动 data_source 标记过的批次）
-- ============================================================
DELETE FROM `score_calculation_details`
WHERE `calculation_id` IN (
    SELECT `id` FROM `score_calculations`
    WHERE `user_id` = @user_id AND `data_source` = '种子数据链路'
);

DELETE FROM `portrait_evaluation_scores`
WHERE `user_id` = @user_id AND `calculation_id` IN (
    SELECT `id` FROM `score_calculations`
    WHERE `user_id` = @user_id AND `data_source` = '种子数据链路'
);

DELETE FROM `score_calculations`
WHERE `user_id` = @user_id AND `data_source` = '种子数据链路';

-- ============================================================
-- 4. 能力维度字典（已存在同编码则保留原名称，不覆盖）
-- ============================================================
INSERT IGNORE INTO `ability_dimensions`
(`dimension_code`, `dimension_name`, `description`, `sort`, `status`) VALUES
('academic',    '学业成绩', '课程成绩与绩点水平',     1, 1),
('competition', '竞赛实践', '学科竞赛与职业技能证书', 2, 1),
('practice',    '社会实践', '志愿服务与公益实践',     3, 1),
('quality',     '综合素质', '技能证书与学生干部任职', 4, 1),
('moral',       '德育素养', '纪律与德育表现',         5, 1);

-- ============================================================
-- 5. 指标树 + 规则版本
--    播种条件：该校「没有可计分的三级指标」（评分引擎只认 level=3 叶子）。
--    这样即使留着孤立的一级/二级空节点，也不影响补齐一棵可算分的完整树；
--    若线上已有三级指标，则完全跳过，绝不覆盖已有配置。
-- ============================================================
SET @active_leaf_count = (SELECT COUNT(*) FROM `evaluation_indicators`
                          WHERE `school_id` = @school_id AND `level` = 3
                            AND `status` = 1 AND `deleted_at` IS NULL);

SET @seed_version = IF(@active_leaf_count = 0,
    GREATEST(
        COALESCE((SELECT MAX(`version`) FROM `indicator_rule_versions`
                  WHERE `school_id` = @school_id AND `deleted_at` IS NULL), 0),
        COALESCE((SELECT MAX(`version`) FROM `evaluation_indicators`
                  WHERE `school_id` = @school_id AND `deleted_at` IS NULL), 0)
    ) + 1,
    NULL);

-- 重新播种前清掉上一轮遗留的种子指标（仅当本次需要播种时执行）
DELETE FROM `score_calculation_details`
WHERE `indicator_id` BETWEEN 901001 AND 901999 AND @seed_version IS NOT NULL;

DELETE FROM `evaluation_indicators`
WHERE `id` BETWEEN 901001 AND 901999 AND @seed_version IS NOT NULL;

-- 5.1 规则版本（tree_snapshot 留空：评分引擎 resolveRuleForSemester 会回退到活表指标，
--     下次管理端「发布规则版本」时再生成正式快照）
INSERT INTO `indicator_rule_versions`
(`school_id`, `semester_id`, `version`, `version_name`, `effective_at`, `created_by`, `tree_snapshot`)
SELECT @school_id, @semester_id, @seed_version,
       CONCAT('种子规则版本 v', @seed_version), NOW(), NULL, NULL
FROM DUAL WHERE @seed_version IS NOT NULL;

-- 5.2 一级指标（权重合计 1.0000 → 各维度目标分合计 100）
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`,
 `weight`, `description`, `dimension_code`, `status`, `version`, `sort`)
SELECT * FROM (
    SELECT 901001 AS `id`, @school_id AS `school_id`, '学业成绩' AS `indicator_name`, 'SEED-A' AS `indicator_code`,
           NULL AS `parent_id`, 1 AS `level`, 'A' AS `path`, 0.4000 AS `weight`,
           '课程成绩与绩点水平' AS `description`, 'academic' AS `dimension_code`,
           1 AS `status`, @seed_version AS `version`, 1 AS `sort`
    UNION ALL SELECT 901002, @school_id, '竞赛实践', 'SEED-B', NULL, 1, 'B', 0.2500,
                     '学科竞赛与职业技能证书', 'competition', 1, @seed_version, 2
    UNION ALL SELECT 901003, @school_id, '社会实践', 'SEED-C', NULL, 1, 'C', 0.2000,
                     '志愿服务与公益实践', 'practice', 1, @seed_version, 3
    UNION ALL SELECT 901004, @school_id, '综合素质', 'SEED-D', NULL, 1, 'D', 0.1000,
                     '技能证书与学生干部任职', 'quality', 1, @seed_version, 4
    UNION ALL SELECT 901005, @school_id, '德育素养', 'SEED-E', NULL, 1, 'E', 0.0500,
                     '纪律与德育表现', 'moral', 1, @seed_version, 5
) t WHERE @seed_version IS NOT NULL;

-- 5.3 二级指标
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`,
 `weight`, `description`, `dimension_code`, `status`, `version`, `sort`)
SELECT * FROM (
    SELECT 901011 AS `id`, @school_id AS `school_id`, '课程成绩' AS `indicator_name`, 'SEED-A-1' AS `indicator_code`,
           901001 AS `parent_id`, 2 AS `level`, 'A-1' AS `path`, 0.2400 AS `weight`,
           '必修课程期末成绩与优秀率' AS `description`, 'academic' AS `dimension_code`,
           1 AS `status`, @seed_version AS `version`, 11 AS `sort`
    UNION ALL SELECT 901012, @school_id, '绩点水平', 'SEED-A-2', 901001, 2, 'A-2', 0.1600,
                     '学分加权绩点是否达标', 'academic', 1, @seed_version, 12
    UNION ALL SELECT 901021, @school_id, '竞赛获奖', 'SEED-B-1', 901002, 2, 'B-1', 0.1500,
                     '学科竞赛获奖等级', 'competition', 1, @seed_version, 21
    UNION ALL SELECT 901022, @school_id, '获奖数量', 'SEED-B-2', 901002, 2, 'B-2', 0.1000,
                     '竞赛/奖学金/荣誉获奖数量', 'competition', 1, @seed_version, 22
    UNION ALL SELECT 901031, @school_id, '志愿服务', 'SEED-C-1', 901003, 2, 'C-1', 0.1200,
                     '志愿时长累计', 'practice', 1, @seed_version, 31
    UNION ALL SELECT 901032, @school_id, '公益实践', 'SEED-C-2', 901003, 2, 'C-2', 0.0800,
                     '公益与社会实践次数', 'practice', 1, @seed_version, 32
    UNION ALL SELECT 901041, @school_id, '技能证书', 'SEED-D-1', 901004, 2, 'D-1', 0.0600,
                     '外语/职业资格证书等级', 'quality', 1, @seed_version, 41
    UNION ALL SELECT 901042, @school_id, '学生任职', 'SEED-D-2', 901004, 2, 'D-2', 0.0400,
                     '学生组织任职层级', 'quality', 1, @seed_version, 42
    UNION ALL SELECT 901051, @school_id, '德育表现', 'SEED-E-1', 901005, 2, 'E-1', 0.0500,
                     '违纪记录与德育表现', 'moral', 1, @seed_version, 51
) t WHERE @seed_version IS NOT NULL;

-- 5.4 三级指标（叶子节点：权重合计 = 所属一级指标权重）
INSERT INTO `evaluation_indicators`
(`id`, `school_id`, `indicator_name`, `indicator_code`, `parent_id`, `level`, `path`,
 `weight`, `description`, `dimension_code`, `status`, `version`, `sort`, `scoring_rule`)
SELECT * FROM (
    SELECT 901111 AS `id`, @school_id AS `school_id`, '必修课平均分' AS `indicator_name`, 'SEED-A-1-1' AS `indicator_code`,
           901011 AS `parent_id`, 3 AS `level`, 'A-1-1' AS `path`, 0.1200 AS `weight`,
           '必修课程期末成绩平均值' AS `description`, 'academic' AS `dimension_code`,
           1 AS `status`, @seed_version AS `version`, 111 AS `sort`,
           '{"type":"AVG","source":"required_course_scores"}' AS `scoring_rule`
    UNION ALL SELECT 901112, @school_id, '课程优秀率', 'SEED-A-1-2', 901011, 3, 'A-1-2', 0.1200,
                     '必修课成绩≥85 的课程占比', 'academic', 1, @seed_version, 112,
                     '{"type":"THRESHOLD","source":"course_excellent_rate","threshold":50,"score":100}'
    UNION ALL SELECT 901121, @school_id, '绩点达标', 'SEED-A-2-1', 901012, 3, 'A-2-1', 0.1600,
                     '学分加权绩点≥3.0 得满分', 'academic', 1, @seed_version, 121,
                     '{"type":"THRESHOLD","source":"weighted_gpa","threshold":3.0,"score":100}'
    UNION ALL SELECT 901211, @school_id, '竞赛等级得分', 'SEED-B-1-1', 901021, 3, 'B-1-1', 0.1500,
                     '按最高竞赛级别计分', 'competition', 1, @seed_version, 211,
                     '{"type":"MAX","source":"competition_scores"}'
    UNION ALL SELECT 901221, @school_id, '获奖数量得分', 'SEED-B-2-1', 901022, 3, 'B-2-1', 0.1000,
                     '每 1 项获奖计 50 分，上限 100', 'competition', 1, @seed_version, 221,
                     '{"type":"COUNT","source":"award_count","perUnit":1,"scorePerUnit":50,"max":100}'
    UNION ALL SELECT 901311, @school_id, '志愿时长达标', 'SEED-C-1-1', 901031, 3, 'C-1-1', 0.1200,
                     '志愿时长≥40 小时得满分', 'practice', 1, @seed_version, 311,
                     '{"type":"THRESHOLD","source":"volunteer_hours","threshold":40,"score":100}'
    UNION ALL SELECT 901321, @school_id, '公益实践次数', 'SEED-C-2-1', 901032, 3, 'C-2-1', 0.0800,
                     '每 1 次公益实践计 50 分，上限 100', 'practice', 1, @seed_version, 321,
                     '{"type":"COUNT","source":"public_welfare_count","perUnit":1,"scorePerUnit":50,"max":100}'
    UNION ALL SELECT 901411, @school_id, '证书等级得分', 'SEED-D-1-1', 901041, 3, 'D-1-1', 0.0600,
                     '按最高证书等级计分', 'quality', 1, @seed_version, 411,
                     '{"type":"MAX","source":"certificate_level"}'
    UNION ALL SELECT 901421, @school_id, '学生干部任职', 'SEED-D-2-1', 901042, 3, 'D-2-1', 0.0400,
                     '按最高任职层级计分', 'quality', 1, @seed_version, 421,
                     '{"type":"MAX","source":"org_position_level"}'
    UNION ALL SELECT 901511, @school_id, '德育表现得分', 'SEED-E-1-1', 901051, 3, 'E-1-1', 0.0500,
                     '无违纪记录得满分', 'moral', 1, @seed_version, 511,
                     '{"type":"THRESHOLD","source":"violation_count","threshold":0,"score":100}'
) t WHERE @seed_version IS NOT NULL;

-- 5.5 解析评分所用的指标版本与规则版本
SET @leaf_version = COALESCE(
    (SELECT `version` FROM `evaluation_indicators`
     WHERE `school_id` = @school_id AND `level` = 3 AND `status` = 1 AND `deleted_at` IS NULL
     ORDER BY `version` DESC, `id` DESC LIMIT 1), 1);

SET @rule_version = COALESCE(
    (SELECT `version` FROM `indicator_rule_versions`
     WHERE `school_id` = @school_id AND `deleted_at` IS NULL
     ORDER BY (`semester_id` <=> @semester_id) DESC, `version` DESC LIMIT 1),
    @leaf_version);

-- ============================================================
-- 6. 课程主数据（工商管理专业课，按学校+编码幂等）
-- ============================================================
INSERT INTO `courses` (`school_id`, `college_id`, `code`, `name`, `credit`, `course_type`, `status`) VALUES
(@school_id, @college_id, 'BA101', '管理学原理',     3.0, '必修', 1),
(@school_id, @college_id, 'BA102', '微观经济学',     3.0, '必修', 1),
(@school_id, @college_id, 'BA103', '市场营销学',     3.0, '必修', 1),
(@school_id, @college_id, 'BA104', '会计学基础',     3.0, '必修', 1),
(@school_id, @college_id, 'BA105', '大学英语（三）', 3.0, '必修', 1),
(@school_id, @college_id, 'BA106', '商务礼仪实训',   1.5, '选修', 1)
ON DUPLICATE KEY UPDATE
    `college_id`  = VALUES(`college_id`),
    `name`        = VALUES(`name`),
    `credit`      = VALUES(`credit`),
    `course_type` = VALUES(`course_type`),
    `status`      = 1,
    `deleted_at`  = NULL;

-- ============================================================
-- 7. 期末成绩明细（gpa_records）——当前学期
--    总学分 16.5 / 加权绩点 3.82 / 加权平均分 88.18
-- ============================================================
INSERT INTO `gpa_records`
(`school_id`, `user_id`, `semester_id`, `course_code`, `course_name`, `course_type`,
 `attempt_no`, `score`, `gpa`, `credit`) VALUES
(@school_id, @user_id, @semester_id, 'BA101', '管理学原理',     '必修', 1, 91.00, 4.10, 3.0),
(@school_id, @user_id, @semester_id, 'BA102', '微观经济学',     '必修', 1, 84.00, 3.40, 3.0),
(@school_id, @user_id, @semester_id, 'BA103', '市场营销学',     '必修', 1, 90.00, 4.00, 3.0),
(@school_id, @user_id, @semester_id, 'BA104', '会计学基础',     '必修', 1, 88.00, 3.80, 3.0),
(@school_id, @user_id, @semester_id, 'BA105', '大学英语（三）', '必修', 1, 86.00, 3.60, 3.0),
(@school_id, @user_id, @semester_id, 'BA106', '商务礼仪实训',   '选修', 1, 92.00, 4.20, 1.5)
ON DUPLICATE KEY UPDATE
    `course_name` = VALUES(`course_name`),
    `course_type` = VALUES(`course_type`),
    `score`       = VALUES(`score`),
    `gpa`         = VALUES(`gpa`),
    `credit`      = VALUES(`credit`),
    `deleted_at`  = NULL,
    `updated_at`  = NOW();

-- ============================================================
-- 8. 期末成绩明细——上一学期（比当前学期低一档：分数 -6、绩点 -0.2）
--    用于「期末成绩」多学期趋势与画像 change 对比
--    总学分 16.5 / 加权绩点 3.62 / 加权平均分 82.18
-- ============================================================
INSERT INTO `gpa_records`
(`school_id`, `user_id`, `semester_id`, `course_code`, `course_name`, `course_type`,
 `attempt_no`, `score`, `gpa`, `credit`)
SELECT * FROM (
    SELECT @school_id AS `school_id`, @user_id AS `user_id`, @prev_semester_id AS `semester_id`,
           'BA101' AS `course_code`, '管理学原理' AS `course_name`, '必修' AS `course_type`,
           1 AS `attempt_no`, 85.00 AS `score`, 3.90 AS `gpa`, 3.0 AS `credit`
    UNION ALL SELECT @school_id, @user_id, @prev_semester_id, 'BA102', '微观经济学',     '必修', 1, 78.00, 3.20, 3.0
    UNION ALL SELECT @school_id, @user_id, @prev_semester_id, 'BA103', '市场营销学',     '必修', 1, 84.00, 3.80, 3.0
    UNION ALL SELECT @school_id, @user_id, @prev_semester_id, 'BA104', '会计学基础',     '必修', 1, 82.00, 3.60, 3.0
    UNION ALL SELECT @school_id, @user_id, @prev_semester_id, 'BA105', '大学英语（三）', '必修', 1, 80.00, 3.40, 3.0
    UNION ALL SELECT @school_id, @user_id, @prev_semester_id, 'BA106', '商务礼仪实训',   '选修', 1, 86.00, 4.00, 1.5
) t WHERE @prev_semester_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    `course_name` = VALUES(`course_name`),
    `course_type` = VALUES(`course_type`),
    `score`       = VALUES(`score`),
    `gpa`         = VALUES(`gpa`),
    `credit`      = VALUES(`credit`),
    `deleted_at`  = NULL,
    `updated_at`  = NOW();

-- ============================================================
-- 9. 学期成绩汇总（semester_gpa_summaries）
--    档案概览「期末成绩」直接读这张表，必须有记录才会渲染
-- ============================================================
INSERT INTO `semester_gpa_summaries`
(`user_id`, `semester_id`, `class_id`, `major_id`, `course_count`, `total_credit`,
 `weighted_gpa`, `average_score`, `rank_in_class`, `rank_in_major`)
VALUES
(@user_id, @semester_id, @class_id, @major_id, 6, 16.50, 3.82, 88.18, 3, 7)
ON DUPLICATE KEY UPDATE
    `class_id`      = VALUES(`class_id`),
    `major_id`      = VALUES(`major_id`),
    `course_count`  = VALUES(`course_count`),
    `total_credit`  = VALUES(`total_credit`),
    `weighted_gpa`  = VALUES(`weighted_gpa`),
    `average_score` = VALUES(`average_score`),
    `rank_in_class` = VALUES(`rank_in_class`),
    `rank_in_major` = VALUES(`rank_in_major`),
    `deleted_at`    = NULL;

INSERT INTO `semester_gpa_summaries`
(`user_id`, `semester_id`, `class_id`, `major_id`, `course_count`, `total_credit`,
 `weighted_gpa`, `average_score`, `rank_in_class`, `rank_in_major`)
SELECT @user_id, @prev_semester_id, @class_id, @major_id, 6, 16.50, 3.62, 82.18, 5, 11
FROM DUAL WHERE @prev_semester_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    `class_id`      = VALUES(`class_id`),
    `major_id`      = VALUES(`major_id`),
    `course_count`  = VALUES(`course_count`),
    `total_credit`  = VALUES(`total_credit`),
    `weighted_gpa`  = VALUES(`weighted_gpa`),
    `average_score` = VALUES(`average_score`),
    `rank_in_class` = VALUES(`rank_in_class`),
    `rank_in_major` = VALUES(`rank_in_major`),
    `deleted_at`    = NULL;

-- ============================================================
-- 10. 评分批次 + 明细 + 画像分——当前学期
--     演示口径：每个三级指标 raw_score = 82 + MOD(id, 10)（82~91 分）
--     维度得分 = Σ(raw_score × weight)，目标分 = 一级指标 weight × 100
-- ============================================================
INSERT INTO `score_calculations`
(`user_id`, `semester_id`, `calculated_at`, `rule_version`, `data_source`, `trigger_type`, `operator_id`, `status`)
VALUES (@user_id, @semester_id, NOW(), @rule_version, '种子数据链路', 1, NULL, 1);

SET @calc_id = LAST_INSERT_ID();

INSERT INTO `score_calculation_details`
(`calculation_id`, `indicator_id`, `dimension_code`, `raw_score`, `weight`, `weighted_score`, `source_archive_ids`)
SELECT @calc_id, leaf.`id`,
       COALESCE(leaf.`dimension_code`, l2.`dimension_code`, l1.`dimension_code`),
       (82 + MOD(leaf.`id`, 10)),
       leaf.`weight`,
       ROUND((82 + MOD(leaf.`id`, 10)) * leaf.`weight`, 2),
       JSON_ARRAY()
FROM `evaluation_indicators` leaf
LEFT JOIN `evaluation_indicators` l2 ON l2.`id` = leaf.`parent_id`
LEFT JOIN `evaluation_indicators` l1 ON l1.`id` = l2.`parent_id`
WHERE leaf.`level` = 3 AND leaf.`status` = 1 AND leaf.`deleted_at` IS NULL
  AND leaf.`version` = @leaf_version
  AND COALESCE(leaf.`dimension_code`, l2.`dimension_code`, l1.`dimension_code`) IS NOT NULL;

INSERT INTO `portrait_evaluation_scores`
(`user_id`, `semester_id`, `calculation_id`, `dimension_code`, `score`, `target_score`,
 `change`, `gap`, `compared_semester_id`, `rule_version`, `evaluated_at`)
SELECT @user_id, @semester_id, @calc_id, t.`dimension_code`, t.`score`, t.`target`,
       NULL, ROUND(t.`target` - t.`score`, 2), NULL, @rule_version, NOW()
FROM (
    SELECT d.`dimension_code`,
           ROUND(SUM(d.`weighted_score`), 2) AS `score`,
           COALESCE((
               SELECT ROUND(i1.`weight` * 100, 2) FROM `evaluation_indicators` i1
               WHERE i1.`level` = 1 AND i1.`dimension_code` = d.`dimension_code`
                 AND i1.`status` = 1 AND i1.`deleted_at` IS NULL
               ORDER BY i1.`sort`, i1.`id` LIMIT 1
           ), 100) AS `target`
    FROM `score_calculation_details` d
    WHERE d.`calculation_id` = @calc_id AND d.`deleted_at` IS NULL
    GROUP BY d.`dimension_code`
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `portrait_evaluation_scores` p
    WHERE p.`user_id` = @user_id AND p.`semester_id` = @semester_id
      AND p.`dimension_code` = t.`dimension_code` AND p.`deleted_at` IS NULL
);

-- ============================================================
-- 11. 评分批次 + 明细 + 画像分——上一学期（用于 change 对比）
--     演示口径：raw_score = 78 + MOD(id, 10)（78~87 分）
-- ============================================================
INSERT INTO `score_calculations`
(`user_id`, `semester_id`, `calculated_at`, `rule_version`, `data_source`, `trigger_type`, `operator_id`, `status`)
SELECT @user_id, @prev_semester_id, NOW(), @rule_version, '种子数据链路', 1, NULL, 1
FROM DUAL WHERE @prev_semester_id IS NOT NULL;

SET @prev_calc_id = IF(@prev_semester_id IS NULL, NULL, LAST_INSERT_ID());

INSERT INTO `score_calculation_details`
(`calculation_id`, `indicator_id`, `dimension_code`, `raw_score`, `weight`, `weighted_score`, `source_archive_ids`)
SELECT @prev_calc_id, leaf.`id`,
       COALESCE(leaf.`dimension_code`, l2.`dimension_code`, l1.`dimension_code`),
       (78 + MOD(leaf.`id`, 10)),
       leaf.`weight`,
       ROUND((78 + MOD(leaf.`id`, 10)) * leaf.`weight`, 2),
       JSON_ARRAY()
FROM `evaluation_indicators` leaf
LEFT JOIN `evaluation_indicators` l2 ON l2.`id` = leaf.`parent_id`
LEFT JOIN `evaluation_indicators` l1 ON l1.`id` = l2.`parent_id`
WHERE @prev_calc_id IS NOT NULL
  AND leaf.`level` = 3 AND leaf.`status` = 1 AND leaf.`deleted_at` IS NULL
  AND leaf.`version` = @leaf_version
  AND COALESCE(leaf.`dimension_code`, l2.`dimension_code`, l1.`dimension_code`) IS NOT NULL;

INSERT INTO `portrait_evaluation_scores`
(`user_id`, `semester_id`, `calculation_id`, `dimension_code`, `score`, `target_score`,
 `change`, `gap`, `compared_semester_id`, `rule_version`, `evaluated_at`)
SELECT @user_id, @prev_semester_id, @prev_calc_id, t.`dimension_code`, t.`score`, t.`target`,
       NULL, ROUND(t.`target` - t.`score`, 2), NULL, @rule_version, NOW()
FROM (
    SELECT d.`dimension_code`,
           ROUND(SUM(d.`weighted_score`), 2) AS `score`,
           COALESCE((
               SELECT ROUND(i1.`weight` * 100, 2) FROM `evaluation_indicators` i1
               WHERE i1.`level` = 1 AND i1.`dimension_code` = d.`dimension_code`
                 AND i1.`status` = 1 AND i1.`deleted_at` IS NULL
               ORDER BY i1.`sort`, i1.`id` LIMIT 1
           ), 100) AS `target`
    FROM `score_calculation_details` d
    WHERE d.`calculation_id` = @prev_calc_id AND d.`deleted_at` IS NULL
    GROUP BY d.`dimension_code`
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `portrait_evaluation_scores` p
    WHERE p.`user_id` = @user_id AND p.`semester_id` = @prev_semester_id
      AND p.`dimension_code` = t.`dimension_code` AND p.`deleted_at` IS NULL
);

-- 12. 用上一学期画像分回填当前学期的 change / compared_semester_id
UPDATE `portrait_evaluation_scores` cur
JOIN `portrait_evaluation_scores` prev
  ON prev.`user_id` = cur.`user_id`
 AND prev.`dimension_code` = cur.`dimension_code`
 AND prev.`semester_id` = @prev_semester_id
 AND prev.`deleted_at` IS NULL
SET cur.`change` = ROUND(cur.`score` - prev.`score`, 2),
    cur.`compared_semester_id` = @prev_semester_id,
    cur.`updated_at` = NOW()
WHERE cur.`user_id` = @user_id
  AND cur.`semester_id` = @semester_id
  AND cur.`deleted_at` IS NULL
  AND @prev_semester_id IS NOT NULL;

COMMIT;

-- ============================================================
-- 13. 验证：三条查询结果即档案概览页应渲染的数据
-- ============================================================
SELECT '=== 1. 学期（档案概览取 is_current=1 的那条） ===' AS `section`;
SELECT `id`, `name`, `start_date`, `end_date`, `is_current`
FROM `semesters`
WHERE `school_id` = @school_id AND `deleted_at` IS NULL
ORDER BY `start_date`, `id`;

SELECT '=== 2. 期末成绩（semester_gpa_summaries） ===' AS `section`;
SELECT g.`semester_id`, s.`name` AS `semester`, g.`course_count`, g.`total_credit`,
       g.`weighted_gpa`, g.`average_score`, g.`rank_in_class`, g.`rank_in_major`
FROM `semester_gpa_summaries` g
JOIN `semesters` s ON s.`id` = g.`semester_id`
WHERE g.`user_id` = @user_id AND g.`deleted_at` IS NULL
ORDER BY g.`semester_id`;

SELECT '=== 3. 多维度画像（portrait_evaluation_scores） ===' AS `section`;
SELECT p.`semester_id`, s.`name` AS `semester`, p.`dimension_code`, d.`dimension_name`,
       p.`score`, p.`target_score`, p.`gap`, p.`change`, p.`rule_version`, p.`evaluated_at`
FROM `portrait_evaluation_scores` p
LEFT JOIN `ability_dimensions` d ON d.`dimension_code` = p.`dimension_code` AND d.`deleted_at` IS NULL
LEFT JOIN `semesters` s ON s.`id` = p.`semester_id`
WHERE p.`user_id` = @user_id AND p.`deleted_at` IS NULL
ORDER BY p.`semester_id`, d.`sort`;

-- 放弃本次写入：把上面的 COMMIT 改成 ROLLBACK 后重新执行即可。
