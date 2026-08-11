-- ============================================================
-- V24: 评价指标表新增说明字段
-- 对应《管理端接口文档》一、指标配置模块 1.2/1.3 的 description 参数
-- ============================================================

ALTER TABLE `evaluation_indicators`
    ADD COLUMN `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '指标说明' AFTER `weight`;
