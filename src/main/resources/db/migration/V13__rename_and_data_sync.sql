-- ============================================================
-- V13: Rename attachment_relations → file_uploads, add data_sync_logs
-- 1. 将 attachment_relations 重命名为 file_uploads（与设计文档一致）
-- 2. 新增 data_sync_logs 外部系统数据同步日志表
-- ============================================================

-- 1. 重命名 attachment_relations → file_uploads
-- 设计文档已使用 file_uploads 作为表名，文档注明"替代原 attachment_relations 表"
-- MySQL 自动更新所有外键引用（V8 career_plans、career_milestones、V12 export_jobs）
RENAME TABLE `attachment_relations` TO `file_uploads`;


-- 2. data_sync_logs — 外部系统数据同步日志表
CREATE TABLE `data_sync_logs` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `sync_type`         VARCHAR(50)     NOT NULL COMMENT '同步类型：user_sync/grade_sync/course_sync/class_sync',
    `sync_batch_no`     VARCHAR(64)     NOT NULL COMMENT '同步批次号（幂等键）',
    `source_system`     VARCHAR(50)     NOT NULL COMMENT '来源系统标识：academic_affairs/hr/wechat_work',
    `sync_direction`    INT NOT NULL DEFAULT 1 COMMENT '同步方向：1=拉取(从外部拉) 2=推送(推给外部)',
    `sync_mode`         INT NOT NULL DEFAULT 1 COMMENT '同步模式：1=全量 2=增量（按时间戳）',
    `total_count`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    `success_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    `fail_details`      JSON            NULL DEFAULT NULL COMMENT '失败详情列表（含原始数据和错误原因）',
    `field_mapping`     JSON            NULL DEFAULT NULL COMMENT '本次同步使用的字段映射',
    `sync_status`       INT NOT NULL DEFAULT 0 COMMENT '0=同步中 1=完成 2=部分成功 3=失败',
    `error_message`     TEXT            NULL DEFAULT NULL COMMENT '整体错误信息',
    `triggered_by`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '触发人 ID（手动触发时），自动同步为 NULL',
    `started_at`        DATETIME        NOT NULL COMMENT '开始时间',
    `completed_at`      DATETIME        NULL DEFAULT NULL COMMENT '完成时间',
    `retention_until`   DATETIME        NULL DEFAULT NULL COMMENT '日志保留截止时间，默认 90 天后过期',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_dsl_school_type_status` (`school_id`, `sync_type`, `sync_status`),
    INDEX `idx_dsl_started_at` (`started_at`),
    INDEX `idx_dsl_retention_until` (`retention_until`),
    UNIQUE KEY `uk_dsl_sync_batch_no` (`sync_batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部系统数据同步日志表';
