-- ============================================================
-- 种子数据：年级字典（dictionaries 表，dict_type='grade'）
--
-- 背景（2026-10-09 问题清单 B-8）：
--   所有「年级」下拉/筛选此前只能硬编码，热力图的 grade 参数无前端入口。
--   clazz.grade 在库中为自由文本（如 "2024级"），本字典补齐后前端可用
--   GET /common/dict?dictType=grade 拉取年级选项。
--
-- 口径：dict_code / dict_name 均与 clazz.grade 自由文本一致（"N级"），
--       前端直接以 dict_code 作为 grade 查询参数即可精确匹配 classes.grade。
--
-- 说明：
--   1. 覆盖 2022级 ~ 2027级（与学期补齐范围 2022-2023 ~ 2027-2028 对应）；
--      如需新增年级，按同结构追加一行即可。
--   2. 幂等：基于 uk_dict_type_code(dict_type, dict_code, is_deleted_null)
--      用 ON DUPLICATE KEY UPDATE 兜底，重复执行不产生重复数据。
-- ============================================================
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
