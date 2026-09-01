-- ============================================================
-- V33: Award Type Config — add requirements/notes columns
-- award_type_configs 表已由 V6 创建，本迁移补充 evaluate_requirements /
-- evaluate_notes 两列，与 archive_type_configs 对齐，供奖项评选说明
-- （AwardService.getGuide）返回结构化必填要求与注意事项。
-- ============================================================

ALTER TABLE `award_type_configs`
    ADD COLUMN `evaluate_requirements` JSON NULL DEFAULT NULL COMMENT '评选必填字段要求列表' AFTER `evaluate_desc`,
    ADD COLUMN `evaluate_notes`       JSON NULL DEFAULT NULL COMMENT '评选注意事项列表' AFTER `evaluate_requirements`;