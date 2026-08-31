-- ============================================================
-- V33: Award Type Config Table
-- 奖项类型配置表（奖项评选说明数据源），独立于 archive_type_configs，
-- 避免奖项类型混入 Fmy 的 archive_category 档案类型下拉。
-- ============================================================

CREATE TABLE `award_type_configs` (
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `award_type`           VARCHAR(50)     NOT NULL COMMENT '奖项类型英文编码',
    `type_name`            VARCHAR(50)     NOT NULL COMMENT '中文名称',
    `evaluate_desc`        TEXT            NULL DEFAULT NULL COMMENT '评选说明正文',
    `evaluate_requirements` JSON           NULL DEFAULT NULL COMMENT '评选必填字段要求列表',
    `evaluate_notes`       JSON            NULL DEFAULT NULL COMMENT '评选注意事项列表',
    `sort`                 INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status`               INT             NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`           TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`      TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_atc_award_type` (`award_type`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项类型配置表';