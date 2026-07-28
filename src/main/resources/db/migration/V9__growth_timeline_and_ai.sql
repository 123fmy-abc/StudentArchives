-- ============================================================
-- V9: Growth Timeline and AI Conversation
-- 成长时间轴、AI对话模块
-- ============================================================

-- 1. growth_timelines — 成长时间轴表
CREATE TABLE `growth_timelines` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `semester_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `event_type`       INT NOT NULL COMMENT '1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升',
    `event_name`       VARCHAR(255)    NOT NULL COMMENT '事件名称',
    `content`          TEXT            NULL DEFAULT NULL COMMENT '事件详细描述/富文本',
    `cover_image`      VARCHAR(500)    NULL DEFAULT NULL COMMENT '封面图片URL',
    `event_at`         DATE            NOT NULL COMMENT '发生时间',
    `source_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '来源记录ID',
    `source_type`      VARCHAR(100)    NULL DEFAULT NULL COMMENT '来源模型类型',
    `event_key`        VARCHAR(64)     NULL DEFAULT NULL COMMENT '业务去重键',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_gt_user_event` (`user_id`, `event_at`),
    INDEX `idx_gt_user_type` (`user_id`, `event_type`),
    INDEX `idx_gt_school_id` (`school_id`),
    INDEX `idx_gt_event_key` (`event_key`),
    UNIQUE KEY `uk_gt_source` (`source_type`, `source_id`, `is_deleted_null`),
    UNIQUE KEY `uk_gt_event_key` (`user_id`, `event_key`, `is_deleted_null`),
    CONSTRAINT `fk_gt_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gt_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长时间轴表';


-- 2. growth_timeline_abilities — 成长时间轴能力维度子表
CREATE TABLE `growth_timeline_abilities` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timeline_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联 growth_timelines.id',
    `dimension_code`  VARCHAR(50)     NOT NULL COMMENT '能力维度编码',
    `score`           DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '该事件带来的维度得分变化',
    `created_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_gta_timeline_dimension` (`timeline_id`, `dimension_code`),
    CONSTRAINT `fk_gta_timeline_id` FOREIGN KEY (`timeline_id`) REFERENCES `growth_timelines` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长时间轴能力维度子表';


-- 3. growth_timeline_tags — 成长时间轴标签子表
CREATE TABLE `growth_timeline_tags` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `timeline_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 growth_timelines.id',
    `tag_name`    VARCHAR(100)    NOT NULL COMMENT '标签名称',
    `created_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`  TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_gtt_timeline_id` (`timeline_id`),
    INDEX `idx_gtt_tag_name` (`tag_name`),
    CONSTRAINT `fk_gtt_timeline_id` FOREIGN KEY (`timeline_id`) REFERENCES `growth_timelines` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长时间轴标签子表';


-- 4. ai_conversations — AI 对话会话表
CREATE TABLE `ai_conversations` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`  BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    `title`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '会话标题',
    `context`    JSON            NULL DEFAULT NULL COMMENT '系统上下文/人设配置',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    `created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后对话时间',
    `deleted_at` TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_aic_user_status` (`user_id`, `status`),
    INDEX `idx_aic_user_updated` (`user_id`, `updated_at`),
    INDEX `idx_aic_school_id` (`school_id`),
    CONSTRAINT `fk_aic_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_aic_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话会话表';


-- 5. ai_messages — AI 对话消息表
CREATE TABLE `ai_messages` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联会话',
    `role`               VARCHAR(20)     NOT NULL COMMENT 'user/assistant/system',
    `content`            TEXT            NOT NULL COMMENT '消息内容',
    `model_name`         VARCHAR(100)    NULL DEFAULT NULL COMMENT '使用的 AI 模型',
    `token_usage`        INT UNSIGNED    NULL DEFAULT NULL COMMENT 'Token 消耗量',
    `generation_time_ms` INT UNSIGNED    NULL DEFAULT NULL COMMENT '生成耗时（毫秒）',
    `created_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`         TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    `retention_until`    DATETIME        NULL DEFAULT NULL COMMENT '数据保留截止时间',
    PRIMARY KEY (`id`),
    INDEX `idx_aim_conversation` (`conversation_id`, `created_at`),
    CONSTRAINT `fk_aim_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话消息表';


-- 6. ai_generation_logs — AI 生成记录表
CREATE TABLE `ai_generation_logs` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    `generation_type`    VARCHAR(50)     NOT NULL COMMENT '生成类型',
    `idempotency_key`    VARCHAR(64)     NULL DEFAULT NULL COMMENT '幂等键',
    `related_type`       VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型',
    `related_id`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录 ID',
    `input_data`         JSON            NULL DEFAULT NULL COMMENT '输入数据摘要（脱敏）',
    `output_content`     TEXT            NOT NULL COMMENT 'AI 生成的内容',
    `model_name`         VARCHAR(100)    NULL DEFAULT NULL COMMENT '使用的 AI 模型',
    `model_version`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '模型版本',
    `prompt_version`     VARCHAR(50)     NULL DEFAULT NULL COMMENT '提示词版本',
    `token_usage`        INT UNSIGNED    NULL DEFAULT NULL COMMENT 'Token 消耗量',
    `generation_time_ms` INT UNSIGNED    NULL DEFAULT NULL COMMENT '生成耗时（毫秒）',
    `call_status`        INT NOT NULL DEFAULT 1 COMMENT '0=失败 1=成功 2=重试',
    `error_msg`          TEXT            NULL DEFAULT NULL COMMENT '调用失败时的错误信息',
    `retry_of`           BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '若为重试，指向原记录 ID',
    `is_used`            INT NOT NULL DEFAULT 0 COMMENT '0=未采用 1=已采用',
    `created_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`         TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`    TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    `retention_until`    DATETIME        NULL DEFAULT NULL COMMENT '数据保留截止时间',
    PRIMARY KEY (`id`),
    INDEX `idx_aigl_user_id` (`user_id`),
    INDEX `idx_aigl_generation_type` (`generation_type`),
    INDEX `idx_aigl_model_name` (`model_name`),
    INDEX `idx_aigl_related` (`related_type`, `related_id`, `call_status`),
    UNIQUE KEY `uk_aigl_idempotency_key` (`idempotency_key`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 生成记录表';


-- 7. ai_teacher_feedbacks — AI 生成教师反馈表
CREATE TABLE `ai_teacher_feedbacks` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `generation_log_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 ai_generation_logs.id',
    `teacher_id`        BIGINT UNSIGNED NOT NULL COMMENT '教师 ID',
    `action`            INT NOT NULL COMMENT '1=采纳 2=修改后采纳 3=拒绝',
    `modified_content`  TEXT            NULL DEFAULT NULL COMMENT '教师修改后的最终内容',
    `reject_reason`     VARCHAR(255)    NULL DEFAULT NULL COMMENT '不采纳原因',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_atf_generation_log_id` (`generation_log_id`),
    INDEX `idx_atf_teacher_id` (`teacher_id`),
    CONSTRAINT `fk_atf_generation_log_id` FOREIGN KEY (`generation_log_id`) REFERENCES `ai_generation_logs` (`id`),
    CONSTRAINT `fk_atf_teacher_id` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 生成教师反馈表';
