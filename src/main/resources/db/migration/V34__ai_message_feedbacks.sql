-- ============================================================
-- V34: AI 消息反馈（学生端）
-- ============================================================

CREATE TABLE `ai_message_feedbacks` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '反馈学生 ID',
    `message_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 ai_messages.id',
    `feedback`         VARCHAR(20)     NOT NULL COMMENT 'useful=有用 useless=无用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aimf_user_message` (`user_id`, `message_id`, `is_deleted_null`),
    INDEX `idx_aimf_message_id` (`message_id`),
    CONSTRAINT `fk_aimf_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_aimf_message_id` FOREIGN KEY (`message_id`) REFERENCES `ai_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 消息反馈表（学生端）';