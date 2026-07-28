-- ============================================================
-- V4: Archive Base and File Management
-- 档案基表、版本历史、类型配置、文件上传、附件限制
-- ============================================================

-- 1. archive_type_configs — 档案类型配置表
CREATE TABLE `archive_type_configs` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_type`        VARCHAR(50)     NOT NULL COMMENT '档案类型英文编码',
    `type_name`           VARCHAR(50)     NOT NULL COMMENT '中文名称',
    `evaluate_desc`       TEXT            NULL DEFAULT NULL COMMENT '评选说明正文',
    `evaluate_requirements` JSON          NULL DEFAULT NULL COMMENT '评选必填字段要求列表',
    `evaluate_notes`      JSON            NULL DEFAULT NULL COMMENT '评选注意事项列表',
    `apply_desc`          TEXT            NULL DEFAULT NULL COMMENT '申报说明',
    `icon`                VARCHAR(100)    NULL DEFAULT NULL COMMENT '图标类名',
    `sort`                INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人 ID',
    `created_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`     TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_atc_archive_type` (`archive_type`, `is_deleted_null`),
    CONSTRAINT `fk_atc_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_atc_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案类型配置表';


-- 2. archives — 档案基表
CREATE TABLE `archives` (
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`              BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`                BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `archive_type`           VARCHAR(50)     NOT NULL COMMENT '档案类型（英文编码）',
    `title`                  VARCHAR(255)    NOT NULL COMMENT '档案具体标题',
    `semester_id`            BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `course_code`            VARCHAR(50)     NULL DEFAULT NULL COMMENT '关联课程编码',
    `obtained_at`            DATE            NULL DEFAULT NULL COMMENT '获得/发生时间',
    `duplicate_check_status` INT NOT NULL DEFAULT 0 COMMENT '0=未检测 1=检测中 2=疑似重复 3=已排除',
    `duplicate_info`         JSON            NULL DEFAULT NULL COMMENT '疑似重复记录ID列表及相似度',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审批 2=通过 3=已退回 4=已撤销',
    `rejected_reason`        TEXT            NULL DEFAULT NULL COMMENT '退回原因',
    `correction_reason`      TEXT            NULL DEFAULT NULL COMMENT '更正原因',
    `submitted_at`           DATETIME        NULL DEFAULT NULL COMMENT '提交时间',
    `audited_at`             DATETIME        NULL DEFAULT NULL COMMENT '审核时间',
    `auditor_id`             BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
    `returned_at`            DATETIME        NULL DEFAULT NULL COMMENT '退回时间',
    `passed_at`              DATETIME        NULL DEFAULT NULL COMMENT '通过时间',
    `revoked_at`             DATETIME        NULL DEFAULT NULL COMMENT '撤销时间',
    `current_version`        INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `submit_count`           INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '提交次数',
    `draft_saved_at`         DATETIME        NULL DEFAULT NULL COMMENT '草稿自动保存时间',
    `created_at`             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`             TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_archives_user_type` (`user_id`, `archive_type`),
    INDEX `idx_archives_status` (`status`),
    INDEX `idx_archives_semester_id` (`semester_id`),
    INDEX `idx_archives_user_obtained` (`user_id`, `obtained_at`),
    INDEX `idx_archives_submitted_at` (`submitted_at`),
    INDEX `idx_archives_school_id` (`school_id`),
    INDEX `idx_archives_type_status` (`archive_type`, `status`),
    INDEX `idx_archives_user_status_time` (`user_id`, `status`, `submitted_at`),
    INDEX `idx_archives_school_status_time` (`school_id`, `status`, `submitted_at`),
    CONSTRAINT `ck_archives_rejected` CHECK (`status` != 3 OR `rejected_reason` IS NOT NULL),
    CONSTRAINT `fk_archives_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_archives_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_archives_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_archives_auditor_id` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案基表';


-- 3. duplicate_detection_rules — 重复申报检测规则配置表
CREATE TABLE `duplicate_detection_rules` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `archive_type`        VARCHAR(50)     NOT NULL COMMENT '档案类型编码',
    `detect_fields`       JSON            NOT NULL COMMENT '检测字段列表',
    `similarity_algorithm` VARCHAR(50)    NOT NULL DEFAULT 'exact' COMMENT '相似度算法：exact/levenshtein/jaccard',
    `similarity_threshold` DECIMAL(3,2)   NOT NULL DEFAULT 1.00 COMMENT '判定为重复的相似度阈值（0.00~1.00）',
    `time_window_days`    INT UNSIGNED    NULL DEFAULT NULL COMMENT '检测时间窗口（天），NULL=全部历史',
    `auto_check`          INT NOT NULL DEFAULT 1 COMMENT '0=手动触发 1=提交时自动检测',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`     TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ddr_school_type` (`school_id`, `archive_type`, `is_deleted_null`),
    CONSTRAINT `fk_ddr_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ddr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重复申报检测规则配置表';


-- 4. model_versions — 通用版本历史表
CREATE TABLE `model_versions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `model_type`       VARCHAR(50)     NOT NULL COMMENT '业务类型：archive/award_application/career_plan/export_template',
    `model_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联业务记录 ID',
    `version`          INT UNSIGNED    NOT NULL COMMENT '版本号',
    `title`            VARCHAR(255)    NULL DEFAULT NULL COMMENT '版本标题',
    `data_snapshot`    JSON            NOT NULL COMMENT '完整数据快照',
    `status` INT NULL DEFAULT NULL COMMENT '该版本状态',
    `rejected_reason`  TEXT            NULL DEFAULT NULL COMMENT '退回原因',
    `change_summary`   VARCHAR(255)    NULL DEFAULT NULL COMMENT '变更说明',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_mv_model` (`model_type`, `model_id`),
    UNIQUE KEY `uk_mv_version` (`model_type`, `model_id`, `version`),
    CONSTRAINT `fk_mv_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用版本历史表';


-- 5. audit_comment_templates — 审核意见模板表
CREATE TABLE `audit_comment_templates` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `template_code`    VARCHAR(50)     NULL DEFAULT NULL COMMENT '模板编码',
    `template_content` TEXT            NOT NULL COMMENT '模板内容',
    `category`         INT NOT NULL DEFAULT 2 COMMENT '1=通过意见 2=退回原因',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `usage_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '使用次数',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_act_school_id` (`school_id`),
    INDEX `idx_act_category` (`category`),
    INDEX `idx_act_status` (`status`),
    INDEX `idx_act_template_code` (`template_code`),
    CONSTRAINT `fk_act_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核意见模板表';


-- 6. file_uploads — 文件上传管理表
CREATE TABLE `file_uploads` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
    `biz_type`          VARCHAR(50)     NULL DEFAULT NULL COMMENT '关联业务类型编码',
    `biz_id`            BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联业务记录ID',
    `file_category`     VARCHAR(50)     NULL DEFAULT NULL COMMENT '附件分类：certificate/photo/proof/other',
    `original_name`     VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    `file_path`         VARCHAR(500)    NOT NULL COMMENT '存储路径',
    `file_size`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '文件大小（字节）',
    `mime_type`         VARCHAR(100)    NULL DEFAULT NULL COMMENT 'MIME类型',
    `disk`              VARCHAR(50)     NOT NULL DEFAULT 'local' COMMENT '存储磁盘',
    `preview_url`       VARCHAR(500)    NULL DEFAULT NULL COMMENT '预览图/预览文件URL',
    `convert_status`    INT NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已生成预览 2=失败',
    `sort_order`        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `deleted_by`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '删除者ID',
    `file_status`       INT NOT NULL DEFAULT 1 COMMENT '1=暂存 2=已绑定 3=已删除（软删标记）',
    `temp_expire_at`    DATETIME        NULL DEFAULT NULL COMMENT '暂存过期时间',
    `download_expire_at` DATETIME       NULL DEFAULT NULL COMMENT '下载有效期截止时间（NULL=永久）',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_fu_biz_type_category` (`biz_type`, `biz_id`, `file_category`),
    INDEX `idx_fu_biz_type` (`biz_type`, `biz_id`),
    INDEX `idx_fu_status_expire` (`file_status`, `temp_expire_at`),
    INDEX `idx_fu_user_id` (`user_id`),
    INDEX `idx_fu_deleted_by` (`deleted_by`),
    CONSTRAINT `fk_fu_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_fu_deleted_by` FOREIGN KEY (`deleted_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传管理表';


-- 7. attachment_limits — 附件上传限制配置表
CREATE TABLE `attachment_limits` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `archive_type`      VARCHAR(50)     NOT NULL COMMENT '档案类型编码',
    `allowed_extensions` JSON           NOT NULL COMMENT '允许的扩展名列表',
    `max_file_size`     BIGINT UNSIGNED NOT NULL DEFAULT 10485760 COMMENT '单个文件最大字节数（默认10MB）',
    `max_files`         INT UNSIGNED    NOT NULL DEFAULT 5 COMMENT '最大文件数量',
    `min_files`         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '最小文件数量（0=可选）',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人 ID',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_al_school_type` (`school_id`, `archive_type`, `is_deleted_null`),
    CONSTRAINT `fk_al_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_al_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_al_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件上传限制配置表';
