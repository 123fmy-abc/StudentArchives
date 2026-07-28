-- ============================================================
-- V10: Messages and Notifications
-- 公告、消息中心、通知设置、消息模板、投递日志
-- ============================================================

-- 1. announcements — 公告/信息发布表
CREATE TABLE `announcements` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `title`        VARCHAR(255)    NOT NULL COMMENT '标题',
    `content`      TEXT            NOT NULL COMMENT '内容',
    `publisher_id` BIGINT UNSIGNED NOT NULL COMMENT '发布人ID',
    `target_type`  VARCHAR(50)     NOT NULL COMMENT '发布对象：all/college/major/class',
    `target_id`    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '目标对象ID',
    `published_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`   TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ann_publisher_id` (`publisher_id`),
    INDEX `idx_ann_target` (`target_type`, `target_id`),
    INDEX `idx_ann_status` (`status`),
    CONSTRAINT `fk_ann_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_ann_publisher_id` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告/信息发布表';


-- 2. announcement_reads — 公告已读记录表
CREATE TABLE `announcement_reads` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `announcement_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 announcements.id',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `read_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `created_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null` TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_anr_read` (`announcement_id`, `user_id`, `is_deleted_null`),
    CONSTRAINT `fk_anr_announcement_id` FOREIGN KEY (`announcement_id`) REFERENCES `announcements` (`id`),
    CONSTRAINT `fk_anr_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';


-- 3. message_templates — 消息模板表
CREATE TABLE `message_templates` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `template_code`    VARCHAR(50)     NOT NULL COMMENT '模板编码',
    `template_name`    VARCHAR(100)    NOT NULL COMMENT '模板名称',
    `category`         VARCHAR(50)     NOT NULL COMMENT '分类：audit_remind/system_notice/dynamic_remind',
    `title_template`   VARCHAR(255)    NOT NULL COMMENT '标题模板',
    `content_template` TEXT            NOT NULL COMMENT '内容模板',
    `variables`        JSON            NULL DEFAULT NULL COMMENT '变量定义',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_mt_category` (`category`),
    UNIQUE KEY `uk_mt_template_code` (`template_code`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';


-- 4. user_messages — 用户消息中心表
CREATE TABLE `user_messages` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
    `sender_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '发送人ID（系统消息为NULL）',
    `sender_type`    INT NOT NULL DEFAULT 1 COMMENT '1=系统 2=人工 3=自动触发',
    `template_id`    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 message_templates.id',
    `category`       VARCHAR(50)     NOT NULL COMMENT '消息分类',
    `title`          VARCHAR(255)    NOT NULL COMMENT '消息标题',
    `content`        TEXT            NULL DEFAULT NULL COMMENT '消息内容',
    `related_type`   VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型类型',
    `related_id`     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录ID',
    `send_channel`   VARCHAR(20)     NOT NULL DEFAULT 'push' COMMENT '发送渠道：push/email/sms',
    `is_read`        INT NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    `read_at`        DATETIME        NULL DEFAULT NULL COMMENT '阅读时间',
    `is_archived`    INT NOT NULL DEFAULT 0 COMMENT '0=未归档 1=已归档',
    `archived_at`    DATETIME        NULL DEFAULT NULL COMMENT '归档时间',
    `is_important`   INT NOT NULL DEFAULT 0 COMMENT '1=重要消息',
    `deadline`       DATETIME        NULL DEFAULT NULL COMMENT '提醒消息的截止日期',
    `jump_url`       VARCHAR(500)    NULL DEFAULT NULL COMMENT '审核消息直接跳转地址',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_um_user_read_archived` (`user_id`, `is_read`, `is_archived`, `deleted_at`),
    INDEX `idx_um_category` (`category`),
    INDEX `idx_um_related` (`related_type`, `related_id`),
    INDEX `idx_um_created_at` (`created_at`),
    INDEX `idx_um_user_archive_read` (`user_id`, `is_archived`, `is_read`),
    CONSTRAINT `fk_um_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_um_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_um_template_id` FOREIGN KEY (`template_id`) REFERENCES `message_templates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息中心表';


-- 5. notification_settings — 用户消息通知设置表
CREATE TABLE `notification_settings` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `category`         VARCHAR(50)     NOT NULL COMMENT '通知分类',
    `email_enabled`    INT NOT NULL DEFAULT 1 COMMENT '0=关闭 1=开启邮件通知',
    `sms_enabled`      INT NOT NULL DEFAULT 0 COMMENT '0=关闭 1=开启短信通知',
    `push_enabled`     INT NOT NULL DEFAULT 1 COMMENT '0=关闭 1=开启站内推送',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ns_user_category` (`user_id`, `category`, `is_deleted_null`),
    CONSTRAINT `fk_ns_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息通知设置表';


-- 6. notification_delivery_logs — 通知投递记录表
CREATE TABLE `notification_delivery_logs` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `message_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 user_messages.id',
    `user_id`            BIGINT UNSIGNED NOT NULL COMMENT '接收用户 ID',
    `channel`            VARCHAR(20)     NOT NULL COMMENT '投递渠道：push/email/sms',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=待投递 1=已投递 2=失败 3=已读（push专用）',
    `fail_reason`        TEXT            NULL DEFAULT NULL COMMENT '失败原因',
    `retry_count`        INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retries`        INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `next_retry_at`      DATETIME        NULL DEFAULT NULL COMMENT '下次重试时间',
    `delivered_at`       DATETIME        NULL DEFAULT NULL COMMENT '投递成功时间',
    `read_at`            DATETIME        NULL DEFAULT NULL COMMENT '阅读时间（push渠道）',
    `channel_message_id` VARCHAR(255)    NULL DEFAULT NULL COMMENT '第三方渠道消息 ID',
    `created_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_ndl_user_channel_status` (`user_id`, `channel`, `status`),
    INDEX `idx_ndl_status_retry` (`status`, `next_retry_at`),
    UNIQUE KEY `uk_ndl_message_channel` (`message_id`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知投递记录表';
