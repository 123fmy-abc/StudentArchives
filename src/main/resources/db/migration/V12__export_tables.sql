-- ============================================================
-- V12: Export and Anonymization Tables
-- 导出模板、模板字段映射、导出任务队列、导出审计、匿名化映射
-- ============================================================

-- 1. export_templates — 导出模板配置表
CREATE TABLE `export_templates` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `template_name`     VARCHAR(100)    NOT NULL COMMENT '模板名称',
    `template_code`     VARCHAR(50)     NOT NULL COMMENT '模板编码',
    `export_type`       VARCHAR(50)     NOT NULL COMMENT '导出业务类型',
    `scope_type`        INT NULL DEFAULT NULL COMMENT '模板默认导出范围：1=全校 2=学院 3=专业 4=班级',
    `fields_config`     JSON            NOT NULL COMMENT '字段列表模式下的列配置',
    `filter_conditions` JSON            NULL DEFAULT NULL COMMENT '默认筛选条件',
    `template_mode`     INT NOT NULL DEFAULT 1 COMMENT '1=字段列表模式 2=自由模板模式',
    `template_content`  LONGTEXT        NULL DEFAULT NULL COMMENT 'HTML 模板源码 / Word 模板 XML',
    `engine_type`       VARCHAR(20)     NOT NULL DEFAULT 'puppeteer' COMMENT '渲染引擎：puppeteer/itext/wkhtmltopdf',
    `page_config`       JSON            NULL DEFAULT NULL COMMENT '页面配置',
    `paper_size`        VARCHAR(20)     NOT NULL DEFAULT 'A4' COMMENT '纸张规格',
    `orientation`       INT NOT NULL DEFAULT 1 COMMENT '1=纵向 2=横向',
    `margin_config`     JSON            NULL DEFAULT NULL COMMENT '边距配置（单位 mm）',
    `header_html`       TEXT            NULL DEFAULT NULL COMMENT '页眉 HTML',
    `footer_html`       TEXT            NULL DEFAULT NULL COMMENT '页脚 HTML',
    `watermark_config`  JSON            NULL DEFAULT NULL COMMENT '水印配置',
    `font_config`       JSON            NULL DEFAULT NULL COMMENT '全局字体、字号、行高、颜色配置',
    `preview_image`     VARCHAR(500)    NULL DEFAULT NULL COMMENT '模板预览图 URL',
    `version`           INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '模板版本号',
    `is_default`        INT NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认模板',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`        BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_et_export_type` (`export_type`),
    INDEX `idx_et_scope_type` (`scope_type`),
    INDEX `idx_et_template_mode` (`template_mode`),
    INDEX `idx_et_is_default` (`is_default`),
    INDEX `idx_et_created_by` (`created_by`),
    UNIQUE KEY `uk_et_template_code` (`school_id`, `template_code`, `is_deleted_null`),
    CONSTRAINT `fk_et_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_et_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出模板配置表';


-- 2. export_template_fields — 模板字段映射表
CREATE TABLE `export_template_fields` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `template_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 export_templates.id',
    `placeholder_key`  VARCHAR(100)    NOT NULL COMMENT '占位符标识',
    `field_source`     VARCHAR(50)     NOT NULL DEFAULT 'table' COMMENT '数据来源：table/variable/compute',
    `field_path`       VARCHAR(255)    NOT NULL COMMENT '字段路径',
    `data_type`        VARCHAR(20)     NOT NULL DEFAULT 'string' COMMENT '数据类型',
    `format_rule`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '格式化规则',
    `default_value`    VARCHAR(255)    NULL DEFAULT NULL COMMENT '字段为空时的默认显示文本',
    `is_list`          INT NOT NULL DEFAULT 0 COMMENT '0=单值 1=列表循环',
    `list_template`    TEXT            NULL DEFAULT NULL COMMENT '列表项 HTML 模板（is_list=1 时必填）',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_etf_placeholder` (`template_id`, `placeholder_key`, `is_deleted_null`),
    CONSTRAINT `fk_etf_template_id` FOREIGN KEY (`template_id`) REFERENCES `export_templates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板字段映射表';


-- 3. export_jobs — 导出任务队列表
CREATE TABLE `export_jobs` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `template_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 export_templates.id',
    `operator_id`       BIGINT UNSIGNED NOT NULL COMMENT '操作人',
    `export_type`       VARCHAR(50)     NOT NULL COMMENT '导出业务类型',
    `scope_type`        INT NULL DEFAULT NULL COMMENT '实际导出范围',
    `scope_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '对应组织表 ID',
    `grade`             VARCHAR(20)     NULL DEFAULT NULL COMMENT '年级筛选',
    `filter_conditions` JSON            NULL DEFAULT NULL COMMENT '实际使用的筛选条件（快照）',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=待处理 1=处理中 2=完成 3=失败',
    `total_count`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '待导出总记录数',
    `success_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功生成数',
    `file_id`           BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 file_uploads.id',
    `error_msg`         TEXT            NULL DEFAULT NULL COMMENT '失败原因',
    `started_at`        DATETIME        NULL DEFAULT NULL COMMENT '开始处理时间',
    `completed_at`      DATETIME        NULL DEFAULT NULL COMMENT '完成时间',
    `expire_at`         DATETIME        NULL DEFAULT NULL COMMENT '下载链接过期时间',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ej_school_status` (`school_id`, `status`),
    INDEX `idx_ej_created_at` (`created_at`),
    INDEX `idx_ej_operator_status_time` (`operator_id`, `status`, `created_at`),
    INDEX `idx_ej_grade` (`grade`),
    CONSTRAINT `fk_ej_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_ej_template_id` FOREIGN KEY (`template_id`) REFERENCES `export_templates` (`id`),
    CONSTRAINT `fk_ej_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_ej_file_id` FOREIGN KEY (`file_id`) REFERENCES `file_uploads` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务队列表';


-- 4. export_operation_logs — 导出操作审计表
CREATE TABLE `export_operation_logs` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `operator_id`       BIGINT UNSIGNED NOT NULL COMMENT '操作人 ID',
    `export_type`       VARCHAR(50)     NOT NULL COMMENT '导出业务类型',
    `action`            INT NOT NULL DEFAULT 1 COMMENT '1=创建 2=下载 3=删除',
    `scope_type`        INT NOT NULL COMMENT '范围类型：1=全校 2=学院 3=专业 4=班级',
    `scope_id`          BIGINT UNSIGNED NOT NULL COMMENT '对应组织表 ID',
    `filter_conditions` JSON            NULL DEFAULT NULL COMMENT '实际使用的筛选条件（快照）',
    `record_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '导出记录数',
    `is_anonymized`     INT NOT NULL DEFAULT 0 COMMENT '0=未脱敏 1=已脱敏',
    `data_version`      INT UNSIGNED    NULL DEFAULT NULL COMMENT '导出数据版本号',
    `field_description` TEXT            NULL DEFAULT NULL COMMENT '导出字段说明（快照）',
    `file_id`           BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 file_uploads.id',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=失败 1=成功',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_eol_school_id` (`school_id`),
    INDEX `idx_eol_operator_id` (`operator_id`),
    INDEX `idx_eol_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出操作审计表';


-- 5. anonymization_maps — 匿名化映射表
CREATE TABLE `anonymization_maps` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `anonymous_code`  VARCHAR(50)     NOT NULL COMMENT '匿名编号（如 ANON_001）',
    `created_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_am_school_user` (`school_id`, `user_id`),
    UNIQUE KEY `uk_am_school_code` (`school_id`, `anonymous_code`),
    CONSTRAINT `fk_am_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_am_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匿名化映射表';
