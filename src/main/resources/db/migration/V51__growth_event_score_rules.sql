-- ============================================================
-- V51: Growth Event Score Rules — 成长事件计分规则表
-- 方案 A：能力得分由管理端按事件类型配置规则自动生成，
-- 学生新增/修改成长时间轴事件时不再自填分数。
--
-- 规则粒度：每条规则对应一种事件类型（event_type 1-6，见 EventTypeEnum），
-- abilities JSON 元素结构：[{"dimensionCode":"xxx","score":10.00}]，
-- dimensionCode 对应 ability_dimensions.dimension_code（展示型引用，非外键）。
-- 无规则的事件类型：事件正常创建，但不产生能力得分行。
-- 存量 growth_timeline_abilities 自填分数保留不动。
-- ============================================================

CREATE TABLE `growth_event_score_rules` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_type`       INT             NOT NULL COMMENT '事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升',
    `abilities`        JSON            NOT NULL COMMENT '该类事件的能力维度得分项：[{"dimensionCode":"xxx","score":10.00}]',
    `remark`           VARCHAR(255)    NULL DEFAULT NULL COMMENT '备注',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gesr_event_type` (`event_type`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长事件计分规则表';
