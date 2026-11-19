-- ============================================================
-- 种子数据：学期表（semesters）
-- 配合 seed_students.sql（学校 id=1，2024 级学生）
--
-- 命名规则：`学年-第N学期`，如 2024-2025-第1学期 表示 2024-2025 学年第 1 学期，
-- 名称中直接写明第几学期，前端展示为"2024-2025第1学期"（见 CommonService.generateSemesterLabel）。
--
-- 说明：
-- 1. 以 2024-09-01 开学为起点，按学期顺序依次补齐到 2026-2027 学年。
-- 2. 当前学期（is_current=1）：2026-2027-第1学期（2026-09-01 ~ 2027-01-15），
--    即当前时间 2026-11 所处的学期；其余学期均为 0。
-- 3. 幂等：先按旧命名（2024-2025-1 / 2025-2026-第1学期 等）改名，再基于
--    uk_semesters_school_name 用 ON DUPLICATE KEY UPDATE 兜底，重复执行不产生重复数据。
-- ============================================================

-- 旧命名统一为"学年-第N学期"（兼容早期 '2024-2025-1' 与 '2025-2026-第1学期' 两种写法）
UPDATE `semesters` SET `name` = '2024-2025-第1学期'
WHERE `school_id` = 1 AND `name` IN ('2024-2025-1', '2024-2025-第1学期');
UPDATE `semesters` SET `name` = '2024-2025-第2学期'
WHERE `school_id` = 1 AND `name` IN ('2024-2025-2', '2024-2025-第2学期');
UPDATE `semesters` SET `name` = '2025-2026-第1学期'
WHERE `school_id` = 1 AND `name` IN ('2025-2026-1', '2025-2026-第1学期');
UPDATE `semesters` SET `name` = '2025-2026-第2学期'
WHERE `school_id` = 1 AND `name` IN ('2025-2026-2', '2025-2026-第2学期');
UPDATE `semesters` SET `name` = '2026-2027-第1学期'
WHERE `school_id` = 1 AND `name` IN ('2026-2027-1', '2026-2027-第1学期');
UPDATE `semesters` SET `name` = '2026-2027-第2学期'
WHERE `school_id` = 1 AND `name` IN ('2026-2027-2', '2026-2027-第2学期');

INSERT INTO `semesters` (`school_id`, `name`, `start_date`, `end_date`, `is_current`, `status`) VALUES
(1, '2024-2025-第1学期', '2024-09-01', '2025-01-15', 0, 1),
(1, '2024-2025-第2学期', '2025-02-17', '2025-07-05', 0, 1),
(1, '2025-2026-第1学期', '2025-09-01', '2026-01-15', 0, 1),
(1, '2025-2026-第2学期', '2026-02-23', '2026-07-05', 0, 1),
(1, '2026-2027-第1学期', '2026-09-01', '2027-01-15', 1, 1),
(1, '2026-2027-第2学期', '2027-02-22', '2027-07-04', 0, 1)
ON DUPLICATE KEY UPDATE
    `start_date` = VALUES(`start_date`),
    `end_date`   = VALUES(`end_date`),
    `is_current` = VALUES(`is_current`),
    `status`     = VALUES(`status`);

-- 兜底：保证同校仅一条当前学期
UPDATE `semesters`
SET `is_current` = 0
WHERE `school_id` = 1 AND `name` <> '2026-2027-第1学期';
