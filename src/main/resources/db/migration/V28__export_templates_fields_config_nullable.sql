-- ============================================================
-- V28: export_templates.fields_config 改为可空
-- 自由模板模式（template_mode=2）下 fields_config 可不传，
-- 原 V12 建表将 fields_config 定义为 NOT NULL，导致创建
-- template_mode=2 的模板时 INSERT 报非空约束违反
-- （GlobalExceptionHandler 兜底返回"数据校验失败"）
-- ============================================================

ALTER TABLE `export_templates`
    MODIFY COLUMN `fields_config` JSON NULL COMMENT '字段列表模式下的列配置（template_mode=2 自由模板模式可空）';
