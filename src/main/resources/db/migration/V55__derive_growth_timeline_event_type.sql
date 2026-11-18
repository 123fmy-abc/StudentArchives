-- ============================================================
-- V55: 成长时间轴 event_type 改由来源记录推导 —— 存量数据回填
--
-- 背景：event_type 自 V53 起已不参与计分，仅用于列表筛选与 tree/ring 视图着色，
-- 但一直由学生在前端自选，可能与来源记录的客观类型不一致（如把社会实践标成奖项）。
-- 现改为后端按来源记录推导（GrowthTimelineEventTypeResolver），学生传入的 eventType
-- 一律忽略。本迁移把存量节点按同一口径对齐，避免历史数据与新规则不一致。
--
-- 口径（与《学生档案系统表》H 节、《学生端接口文档》4.2 一致）：
--   1=奖项   2=成绩   3=实践   4=职业规划   5=短板改进   6=能力提升
--
-- 说明：
--   1. 仅修正可推导的来源；无法匹配的档案类型（历史脏数据 / 未登记的新类型）
--      保持原 event_type 不变，由应用层在下次编辑时暴露并提示维护映射。
--   2. 别名 archive / award_application 与规范编码一并处理。
-- ============================================================

-- 1. 固定来源类型 → 事件类型
UPDATE `growth_timelines` SET `event_type` = 1
 WHERE `source_type` IN ('award_applications', 'award_application', 'award_competition_stars', 'archive_competitions');

UPDATE `growth_timelines` SET `event_type` = 2
 WHERE `source_type` IN ('semester_gpa_summaries', 'gpa_records');

UPDATE `growth_timelines` SET `event_type` = 3
 WHERE `source_type` IN ('archive_social_practices', 'archive_internships', 'archive_organizations');

UPDATE `growth_timelines` SET `event_type` = 4
 WHERE `source_type` IN ('career_plans', 'career_plan_feedbacks');

UPDATE `growth_timelines` SET `event_type` = 5
 WHERE `source_type` IN ('weakness_analyses', 'improvement_suggestions');

UPDATE `growth_timelines` SET `event_type` = 6
 WHERE `source_type` = 'portrait_evaluation_scores';

-- 2. 通用档案来源：按 archives.archive_type 细分
--    键同时覆盖 ArchiveTypeEnum 完整编码与历史种子数据的短编码（口径同《学生端接口文档》12.1）
UPDATE `growth_timelines` t
JOIN `archives` a ON a.`id` = t.`source_id` AND a.`deleted_at` IS NULL
SET t.`event_type` = CASE
    WHEN a.`archive_type` IN ('academic_competition', 'competition',
                              'scholarship',
                              'honor_certificate', 'certificate') THEN 1
    WHEN a.`archive_type` IN ('social_practice', 'internship', 'organization',
                              'training_project', 'training',
                              'innovation_entrepreneurship', 'innovation') THEN 3
    WHEN a.`archive_type` IN ('academic_research', 'research', 'book_review') THEN 6
    ELSE t.`event_type`
END
WHERE t.`source_type` IN ('archives', 'archive');
