-- ============================================================
-- V33: Award Type Config Table — 新增奖项评选说明字段
-- 奖项类型配置表（奖项评选说明数据源），独立于 archive_type_configs，
-- 避免奖项类型混入 Fmy 的 archive_category 档案类型下拉。
--
-- 注意：award_type_configs 表已由 V6__award_tables.sql 创建并已有数据，
-- 此处只能 ALTER 增量加列，不能重复 CREATE TABLE。
-- 新增字段供奖项评选说明（AwardService.getGuide）使用：
--   evaluate_requirements 评选必填字段要求列表（JSON 对象数组）
--   evaluate_notes        评选注意事项列表（JSON 字符串数组）
-- ============================================================

ALTER TABLE `award_type_configs`
    ADD COLUMN `evaluate_requirements` JSON NULL DEFAULT NULL COMMENT '评选必填字段要求列表' AFTER `evaluate_desc`,
    ADD COLUMN `evaluate_notes` JSON NULL DEFAULT NULL COMMENT '评选注意事项列表' AFTER `evaluate_requirements`;
