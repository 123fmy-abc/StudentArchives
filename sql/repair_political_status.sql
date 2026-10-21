-- ============================================================
-- 修复脚本：存量政治面貌数据（中文名 → 字典编码）
-- ============================================================
-- 背景：seed_students.sql 旧版本把政治面貌显示名（如 '共青团员'）直接写入
--   student_profiles.political_status，而字典表（seed_dictionaries.sql）的
--   dict_code 是编码（league_member）。导致 /profile/info 返回
--   politicalStatusLabel = null。
-- 本脚本将存量中文名统一替换为对应字典编码。
-- 需先执行 seed_dictionaries.sql（若字典数据缺失，请先补种）。
-- ============================================================

UPDATE `student_profiles`
SET `political_status` = 'league_member'
WHERE `political_status` = '共青团员';

UPDATE `student_profiles`
SET `political_status` = 'masses'
WHERE `political_status` = '群众';

UPDATE `student_profiles`
SET `political_status` = 'party_member'
WHERE `political_status` = '中共党员';

UPDATE `student_profiles`
SET `political_status` = 'party_member_pre'
WHERE `political_status` = '中共预备党员';

UPDATE `student_profiles`
SET `political_status` = 'other_party'
WHERE `political_status` = '民主党派';

-- ============================================================
-- 校验：修复后应返回空集（无中文名残留）
-- ============================================================
SELECT `id`, `user_id`, `political_status`
FROM `student_profiles`
WHERE `political_status` IN ('共青团员', '群众', '中共党员', '中共预备党员', '民主党派');
