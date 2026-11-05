-- ============================================================
-- V48: 数据回填 — 补齐学期并回填档案 semester_id
--
-- 背景：
--   semesters 表为空、archives.semester_id 为 NULL 时，
--   统计快照刷新因「未找到当前学期」失败，工作台「档案数据概览」
--   与「档案查看」实时聚合结论矛盾（看板全 0 vs 实际有档案）。
--
-- 本迁移幂等、可安全重复执行：
--   1) 为「无任何非软删学期」的有效学校补种一个当前学期；
--   2) 将 semester_id 为 NULL 的有效档案回填到该校当前学期。
--
-- 注意：执行后如需看板的画像维度/类型分布等「快照类」字段补全，
--       再触发一次统计快照刷新（POST /admin/statistics/refresh，或等待每日调度）。
--       KPI（学生/档案/待审批/已通过）已由代码实时兜底，无需依赖快照。
-- ============================================================

-- 1) 补种当前学期（仅对无任何非软删学期的有效学校）
INSERT INTO `semesters` (`school_id`, `name`, `start_date`, `end_date`, `is_current`, `status`)
SELECT s.`id`, '2025-2026-2', '2026-02-23', '2026-07-05', 1, 1
FROM `schools` s
WHERE s.`deleted_at` IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM `semesters` t
    WHERE t.`school_id` = s.`id` AND t.`deleted_at` IS NULL
  );

-- 2) 回填 semester_id 为 NULL 的有效档案到该校当前学期
UPDATE `archives` a
JOIN `semesters` t
    ON t.`school_id` = a.`school_id`
   AND t.`is_current` = 1
   AND t.`deleted_at` IS NULL
SET a.`semester_id` = t.`id`
WHERE a.`semester_id` IS NULL
  AND a.`deleted_at` IS NULL;
