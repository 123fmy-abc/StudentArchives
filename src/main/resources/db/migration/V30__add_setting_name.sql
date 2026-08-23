-- ============================================================
-- 系统配置新增配置名称字段（《管理端接口文档》十二、12.1 响应含 settingName）
-- ============================================================
ALTER TABLE `system_settings`
    ADD COLUMN `setting_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '配置名称' AFTER `setting_key`;