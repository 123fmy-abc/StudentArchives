-- ============================================================
-- 种子数据：学期补齐（semesters 表）
--
-- 背景（2026-10-09 问题清单 B-9）：
--   GET /common/semesters 此前仅 5 条（2024-2025 ~ 2026-2027），老材料补录
--   与后续学年提交会被卡住。本脚本补齐 2022-2023、2023-2024、2027-2028
--   三个学年的上下学期，共 6 条，school_id=1（华中科技大学，与 seed_semesters.sql 一致）。
--
-- 说明：
--   1. is_current 全部为 0 —— 当前学期仍以库中既有 is_current=1 的那条为准；
--      不新增 is_current=1 的条目，避免破坏「仅一条当前学期」的口径。
--   2. 满足 ck_semesters_date（end_date > start_date）与
--      uk_semesters_school_name(school_id, name, is_deleted_null)。
--   3. 幂等：基于 uk_semesters_school_name 用 ON DUPLICATE KEY UPDATE 兜底，
--      重复执行只更新起止日期/状态，不产生重复数据。
-- ============================================================
INSERT INTO `semesters` (`school_id`, `name`, `start_date`, `end_date`, `is_current`, `status`) VALUES
(1, '2022-2023-1', '2022-09-01', '2023-01-15', 0, 1),
(1, '2022-2023-2', '2023-02-20', '2023-07-05', 0, 1),
(1, '2023-2024-1', '2023-09-01', '2024-01-15', 0, 1),
(1, '2023-2024-2', '2024-02-19', '2024-07-05', 0, 1),
(1, '2027-2028-1', '2027-09-01', '2028-01-15', 0, 1),
(1, '2027-2028-2', '2028-02-21', '2028-07-05', 0, 1)
ON DUPLICATE KEY UPDATE
    `start_date` = VALUES(`start_date`),
    `end_date`   = VALUES(`end_date`),
    `is_current` = VALUES(`is_current`),
    `status`     = VALUES(`status`);
