-- ============================================================
-- V43: form_submission_snapshots 表单提交快照表
-- 叠加式方案A：原有 10 张强类型扩展表（archive_competition 等）仍是唯一主数据源，
-- 本表仅在业务写入成功后，于同一事务内顺带落一份 DTO 序列化 JSON 快照，用于归档/历史留痕。
-- 快照为「单向生成」（旧表写完 → 生成快照），失败随事务回滚，不影响业务正确性。
-- 同一档案可能多次更正产生多份快照，故不设唯一键；保留软删除列以对齐 BaseEntity 写法。
-- ============================================================

CREATE TABLE `form_submission_snapshots` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `category`         VARCHAR(50)     NOT NULL COMMENT '适用类别：archive/award/career_plan',
    `code`             VARCHAR(50)     NOT NULL COMMENT '档案类型编码，对应 archive_type_configs.archive_type',
    `template_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '生成快照所用模板 ID，关联 form_templates.id',
    `template_version` INT UNSIGNED    NULL DEFAULT NULL COMMENT '生成快照时模板版本号',
    `business_type`    VARCHAR(50)     NOT NULL COMMENT '业务类型，如 archive',
    `business_id`      BIGINT UNSIGNED NOT NULL COMMENT '业务主键，如 archives.id',
    `data`             JSON            NOT NULL COMMENT '提交请求 DTO 序列化快照',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_fss_biz` (`business_type`, `business_id`),
    INDEX `idx_fss_template` (`template_id`),
    INDEX `idx_fss_school_code` (`school_id`, `category`, `code`),
    CONSTRAINT `fk_fss_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_fss_template_id` FOREIGN KEY (`template_id`) REFERENCES `form_templates` (`id`),
    CONSTRAINT `fk_fss_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单提交快照表';
