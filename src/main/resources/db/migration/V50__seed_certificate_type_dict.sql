-- ============================================================
-- 种子数据：荣誉证书类型字典（dictionaries，dict_type='certificate_type'）
--
-- 背景：
--   archive_certificates.certificate_type 存的是字典编码（string dict_code），
--   详情接口通过 dict_type='certificate_type' 反查 certificateTypeLabel
--   （见 ActivityService / AdminArchiveService 的 resolveLabel）。
--   原 sql/seed_dictionaries.sql 未预置该字典，导致前端下拉无选项、
--   标签解析结果为 null。
--
-- 说明：
--   与 sql/seed_dictionaries.sql 第 13 节口径一致，幂等可重复执行。
-- ============================================================
INSERT INTO `dictionaries` (`dict_type`, `dict_code`, `dict_name`, `sort`, `status`) VALUES
('certificate_type', 'language',     '语言等级证书',   1, 1),
('certificate_type', 'computer',     '计算机等级证书', 2, 1),
('certificate_type', 'mandarin',     '普通话等级证书', 3, 1),
('certificate_type', 'professional', '职业资格证书',   4, 1),
('certificate_type', 'skill',        '职业技能证书',   5, 1),
('certificate_type', 'honor',        '荣誉获奖证书',   6, 1),
('certificate_type', 'other',        '其他',           7, 1)
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `sort`      = VALUES(`sort`),
    `status`    = VALUES(`status`);
