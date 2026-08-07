-- ============================================================
-- V19: export_jobs.template_id 改为可空
-- 学生端档案导出（POST /profile/export）不涉及导出模板，
-- 原 V12 建表将 template_id 定义为 NOT NULL，导致
-- ProfileExportService 创建任务时 INSERT 报非空约束违反
-- ============================================================

ALTER TABLE `export_jobs`
    MODIFY COLUMN `template_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 export_templates.id（学生端档案导出不涉及模板，可空）';
