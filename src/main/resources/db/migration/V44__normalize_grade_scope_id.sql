-- ============================================================
-- 统一年级数据范围口径（BE-PENDING-006）
--
-- 背景：
--   role_scopes.scope_type=6 表示年级授权，但 scope_id 为 BIGINT，
--   而 classes.grade 是自由文本（如 "2024级"），历史数据存在两种写法：
--     1) scope_id = 2024（年级数字），旧代码按 "N级" 拼字符串；
--     2) 其它无法解析的值，旧代码一律当作“全校”放行。
--
-- 本次口径：
--   scope_type=6 时 scope_id 指向 dictionaries 中 dict_type='grade' 的主键，
--   其 dict_code 即 classes.grade 文本（见 sql/seed_grade_dict.sql）。
--
-- 处理策略：
--   1. 可映射的历史年级授权（scope_id=年份 → "年份级" 字典项）转换为字典主键；
--   2. 无法映射的年级授权停用（fail closed），避免继续被当作全校范围使用。
--      执行前请先用下方核查 SQL 导出待停用名单，人工确认后重建授权。
--
-- 核查 SQL（只读，建议上线前执行）：
--   SELECT rs.id, rs.user_id, rs.scope_type, rs.scope_id, rs.status
--   FROM role_scopes rs
--   WHERE rs.scope_type = 6
--     AND NOT EXISTS (SELECT 1 FROM dictionaries d
--                     WHERE d.id = rs.scope_id AND d.dict_type = 'grade');
-- ============================================================

-- 0) 确保年级字典存在（与 sql/seed_grade_dict.sql 口径一致，幂等）
--    缺少该字典时，管理端无法配置年级授权，历史年份写法也无法映射。
INSERT INTO `dictionaries` (`dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
('grade', '2022级', '2022级', 1, 1),
('grade', '2023级', '2023级', 2, 1),
('grade', '2024级', '2024级', 3, 1),
('grade', '2025级', '2025级', 4, 1),
('grade', '2026级', '2026级', 5, 1),
('grade', '2027级', '2027级', 6, 1)
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `sort`      = VALUES(`sort`),
    `status`    = VALUES(`status`);

-- 1) 历史“年份数字”写法 → 年级字典主键
UPDATE `role_scopes` rs
JOIN `dictionaries` d
  ON d.`dict_type` = 'grade'
 AND d.`dict_code` = CONCAT(rs.`scope_id`, '级')
 AND d.`status` = 1
 AND d.`deleted_at` IS NULL
SET rs.`scope_id` = d.`id`
WHERE rs.`scope_type` = 6;

-- 2) 无法映射的年级授权停用（不再按全校放行）
UPDATE `role_scopes` rs
SET rs.`status` = 0
WHERE rs.`scope_type` = 6
  AND NOT EXISTS (
      SELECT 1 FROM `dictionaries` d
      WHERE d.`id` = rs.`scope_id`
        AND d.`dict_type` = 'grade'
        AND d.`status` = 1
        AND d.`deleted_at` IS NULL
  );
