-- ============================================================
-- V8: Career Planning and Weakness Analysis
-- 职业规划、短板识别与改进建议
-- ============================================================

-- 1. weakness_analyses — 短板识别表
CREATE TABLE `weakness_analyses` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `weakness_type`    VARCHAR(100)    NOT NULL COMMENT '短板类型',
    `weakness_desc`    TEXT            NULL DEFAULT NULL COMMENT '短板描述',
    `source`           INT NOT NULL DEFAULT 1 COMMENT '1=AI生成 2=教师建议',
    `teacher_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '教师ID（source=2时必填）',
    `severity_level`   INT NOT NULL DEFAULT 1 COMMENT '严重程度 1-5',
    `target_score`     INT UNSIGNED    NULL DEFAULT NULL COMMENT '目标分数',
    `is_read`          INT NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    `related_type`     VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型',
    `related_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_wa_user_id` (`user_id`),
    INDEX `idx_wa_source` (`source`),
    INDEX `idx_wa_teacher_id` (`teacher_id`),
    INDEX `idx_wa_related` (`related_type`, `related_id`),
    CONSTRAINT `ck_wa_severity` CHECK (`severity_level` BETWEEN 1 AND 5),
    CONSTRAINT `fk_wa_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_wa_teacher_id` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短板识别表';


-- 2. weakness_progress — 短板改进进度跟踪表
CREATE TABLE `weakness_progress` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `weakness_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 weakness_analyses.id',
    `progress_value` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '进度值 0-100',
    `progress_desc`  VARCHAR(255)    NULL DEFAULT NULL COMMENT '进度描述',
    `recorded_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_wp_weakness_id` (`weakness_id`),
    INDEX `idx_wp_recorded_at` (`recorded_at`),
    CONSTRAINT `ck_wp_progress` CHECK (`progress_value` <= 100),
    CONSTRAINT `fk_wp_weakness_id` FOREIGN KEY (`weakness_id`) REFERENCES `weakness_analyses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短板改进进度跟踪表';


-- 3. career_plans — 职业规划主表
-- 创建时不包含 ai_suggestion_id 外键（依赖 improvement_suggestions 表）
CREATE TABLE `career_plans` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `current_version`  INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `submit_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '提交次数',
    `semester_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `title`            VARCHAR(255)    NOT NULL COMMENT '规划标题',
    `content`          TEXT            NULL DEFAULT NULL COMMENT '规划内容正文',
    `requirement`      TEXT            NULL DEFAULT NULL COMMENT '要求/目标',
    `copy_from_id`     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '复制自某条规划ID',
    `source`           INT NOT NULL DEFAULT 1 COMMENT '1=手动创建 2=AI建议添加',
    `ai_suggestion_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 improvement_suggestions.id',
    `require_confirm`  INT NOT NULL DEFAULT 1 COMMENT '0=直接添加 1=返回草稿待确认',
    `progress_rate`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '整体进度百分比 0-100',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审批 2=通过 3=已退回 4=已撤销',
    `submitted_at`     DATETIME        NULL DEFAULT NULL COMMENT '提交时间',
    `audited_at`       DATETIME        NULL DEFAULT NULL COMMENT '审核时间',
    `auditor_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
    `rejected_reason`  TEXT            NULL DEFAULT NULL COMMENT '退回原因',
    `returned_at`      DATETIME        NULL DEFAULT NULL COMMENT '退回时间',
    `passed_at`        DATETIME        NULL DEFAULT NULL COMMENT '通过时间',
    `revoked_at`       DATETIME        NULL DEFAULT NULL COMMENT '撤销时间',
    `file_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联文件ID',
    `draft_saved_at`   DATETIME        NULL DEFAULT NULL COMMENT '草稿自动保存时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cp_user_semester` (`user_id`, `semester_id`),
    INDEX `idx_cp_status` (`status`),
    INDEX `idx_cp_school_id` (`school_id`),
    INDEX `idx_cp_user_status` (`user_id`, `status`),
    INDEX `idx_cp_ai_suggestion_id` (`ai_suggestion_id`),
    CONSTRAINT `fk_cp_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_cp_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_cp_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
    CONSTRAINT `fk_cp_auditor_id` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_cp_file_id` FOREIGN KEY (`file_id`) REFERENCES `attachment_relations` (`id`) ON DELETE SET NULL
    -- fk_cp_ai_suggestion_id 在 improvement_suggestions 表创建后通过 ALTER TABLE 添加
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业规划主表';


-- 4. career_goals — 职业规划目标子表
CREATE TABLE `career_goals` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `career_plan_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 career_plans.id',
    `goal_title`        VARCHAR(255)    NOT NULL COMMENT '目标标题',
    `goal_desc`         TEXT            NULL DEFAULT NULL COMMENT '目标描述',
    `target_date`       DATE            NULL DEFAULT NULL COMMENT '目标日期',
    `source`            INT NOT NULL DEFAULT 1 COMMENT '1=手动创建 2=AI建议添加',
    `ai_suggestion_id`  BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 AI 建议 ID',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已完成',
    `sort`              INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cg_career_plan_id` (`career_plan_id`),
    CONSTRAINT `fk_cg_career_plan_id` FOREIGN KEY (`career_plan_id`) REFERENCES `career_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业规划目标子表';


-- 5. career_actions — 规划行动子表
CREATE TABLE `career_actions` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `goal_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 career_goals.id',
    `action_title`      VARCHAR(255)    NOT NULL COMMENT '行动标题',
    `action_desc`       TEXT            NULL DEFAULT NULL COMMENT '行动描述',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已完成',
    `source`            INT NOT NULL DEFAULT 1 COMMENT '1=手动创建 2=AI建议添加',
    `ai_suggestion_id`  BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 AI 建议 ID',
    `start_date`        DATE            NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`          DATE            NULL DEFAULT NULL COMMENT '结束日期',
    `completion_rate`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '完成百分比 0-100',
    `sort`              INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ca_goal_status` (`goal_id`, `status`, `sort`),
    CONSTRAINT `fk_ca_goal_id` FOREIGN KEY (`goal_id`) REFERENCES `career_goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规划行动子表';


-- 6. career_milestones — 规划行动里程碑子表
CREATE TABLE `career_milestones` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `action_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 career_actions.id',
    `milestone_title`  VARCHAR(255)    NOT NULL COMMENT '里程碑标题',
    `milestone_date`   DATE            NULL DEFAULT NULL COMMENT '计划完成日期',
    `is_achieved`      INT NOT NULL DEFAULT 0 COMMENT '0=未完成 1=已完成',
    `achieved_at`      DATETIME        NULL DEFAULT NULL COMMENT '实际完成时间',
    `proof_file_id`    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '成果证明材料',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cm_action_id` (`action_id`),
    INDEX `idx_cm_action_achieved` (`action_id`, `is_achieved`),
    INDEX `idx_cm_proof_file_id` (`proof_file_id`),
    CONSTRAINT `fk_cm_action_id` FOREIGN KEY (`action_id`) REFERENCES `career_actions` (`id`),
    CONSTRAINT `fk_cm_proof_file_id` FOREIGN KEY (`proof_file_id`) REFERENCES `attachment_relations` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规划行动里程碑子表';


-- 7. career_reflections — 规划反思子表
CREATE TABLE `career_reflections` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `career_plan_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 career_plans.id',
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `semester_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `reflection_content` TEXT           NOT NULL COMMENT '反思内容',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cr_career_plan_id` (`career_plan_id`),
    CONSTRAINT `fk_cr_career_plan_id` FOREIGN KEY (`career_plan_id`) REFERENCES `career_plans` (`id`),
    CONSTRAINT `fk_cr_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规划反思子表';


-- 8. career_plan_feedbacks — 规划教师反馈子表
CREATE TABLE `career_plan_feedbacks` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `career_plan_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联 career_plans.id',
    `teacher_id`       BIGINT UNSIGNED NOT NULL COMMENT '教师ID',
    `feedback_content` TEXT            NOT NULL COMMENT '反馈内容',
    `suggestion_items` JSON            NULL DEFAULT NULL COMMENT '建议项列表',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cpf_career_plan_id` (`career_plan_id`),
    CONSTRAINT `fk_cpf_career_plan_id` FOREIGN KEY (`career_plan_id`) REFERENCES `career_plans` (`id`),
    CONSTRAINT `fk_cpf_teacher_id` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规划教师反馈子表';


-- 9. improvement_suggestions — 改进建议表
CREATE TABLE `improvement_suggestions` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `weakness_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 weakness_analyses.id',
    `suggestion_type`   VARCHAR(50)     NULL DEFAULT NULL COMMENT '建议类型',
    `related_goal_id`   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 career_goals.id',
    `suggestion_content` TEXT           NOT NULL COMMENT '建议内容',
    `source`            INT NOT NULL DEFAULT 1 COMMENT '1=AI生成 2=教师建议',
    `teacher_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '教师ID',
    `is_implemented`    INT NOT NULL DEFAULT 0 COMMENT '0=未落实 1=已落实',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_is_weakness_id` (`weakness_id`),
    INDEX `idx_is_source` (`source`),
    INDEX `idx_is_teacher_id` (`teacher_id`),
    INDEX `idx_is_related_goal_id` (`related_goal_id`),
    CONSTRAINT `fk_is_weakness_id` FOREIGN KEY (`weakness_id`) REFERENCES `weakness_analyses` (`id`),
    CONSTRAINT `fk_is_teacher_id` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_is_related_goal_id` FOREIGN KEY (`related_goal_id`) REFERENCES `career_goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='改进建议表';


-- 10. 补充 career_plans 对 improvement_suggestions 的外键
ALTER TABLE `career_plans`
    ADD CONSTRAINT `fk_cp_ai_suggestion_id` FOREIGN KEY (`ai_suggestion_id`) REFERENCES `improvement_suggestions` (`id`);
