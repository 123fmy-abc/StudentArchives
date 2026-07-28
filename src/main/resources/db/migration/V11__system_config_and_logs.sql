-- ============================================================
-- V11: System Configuration and Log Tables
-- 系统配置、数据字典、定时任务、审计日志
-- ============================================================

-- 1. system_settings — 系统配置表
CREATE TABLE `system_settings` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `setting_key`      VARCHAR(100)    NOT NULL COMMENT '配置键',
    `setting_value`    TEXT            NOT NULL COMMENT '配置值',
    `setting_group`    VARCHAR(50)     NOT NULL COMMENT '配置分组',
    `value_type`       VARCHAR(20)     NOT NULL COMMENT '值类型：string/int/float/json/boolean',
    `description`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '配置说明',
    `is_editable`      INT NOT NULL DEFAULT 1 COMMENT '0=只读 1=可编辑',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ss_setting_group` (`setting_group`),
    UNIQUE KEY `uk_ss_setting_key` (`setting_key`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';


-- 2. dictionaries — 数据字典表
CREATE TABLE `dictionaries` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `dict_type`        VARCHAR(50)     NOT NULL COMMENT '字典类型编码',
    `dict_code`        VARCHAR(50)     NOT NULL COMMENT '字典项编码',
    `dict_name`        VARCHAR(100)    NOT NULL COMMENT '字典项名称',
    `parent_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '父级ID',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_dict_dict_type` (`dict_type`),
    INDEX `idx_dict_parent_id` (`parent_id`),
    INDEX `idx_dict_type_status` (`dict_type`, `status`),
    UNIQUE KEY `uk_dict_type_code` (`dict_type`, `dict_code`, `is_deleted_null`),
    CONSTRAINT `fk_dict_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `dictionaries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';


-- 3. form_templates — 表单自定义模板表
CREATE TABLE `form_templates` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `template_name`    VARCHAR(100)    NOT NULL COMMENT '模板名称',
    `code`             VARCHAR(50)     NOT NULL COMMENT '模板编码',
    `category`         VARCHAR(50)     NOT NULL DEFAULT 'archive' COMMENT '适用类别：archive/award/career_plan',
    `description`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '模板说明',
    `fields`           JSON            NOT NULL COMMENT '字段配置数组',
    `layout_config`    JSON            NULL DEFAULT NULL COMMENT '布局配置',
    `applicable_roles` JSON            NULL DEFAULT NULL COMMENT '适用角色编码数组',
    `is_default`       INT NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认模板',
    `version`          INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '版本号',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NOT NULL COMMENT '创建人ID',
    `updated_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ft_school_id` (`school_id`),
    INDEX `idx_ft_category` (`category`),
    INDEX `idx_ft_is_default` (`is_default`),
    INDEX `idx_ft_status` (`status`),
    INDEX `idx_ft_created_by` (`created_by`),
    UNIQUE KEY `uk_ft_school_code_category` (`school_id`, `code`, `category`, `is_deleted_null`),
    CONSTRAINT `fk_ft_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_ft_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单自定义模板表';


-- 4. scheduled_tasks — 通用定时任务调度表
CREATE TABLE `scheduled_tasks` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `task_name`        VARCHAR(100)    NOT NULL COMMENT '任务名称',
    `task_code`        VARCHAR(50)     NOT NULL COMMENT '任务编码',
    `task_group`       VARCHAR(50)     NOT NULL COMMENT '任务分组：system/data/notification/cleanup',
    `cron_expression`  VARCHAR(100)    NOT NULL COMMENT 'Cron 表达式',
    `task_handler`     VARCHAR(255)    NOT NULL COMMENT '处理器标识',
    `task_params`      JSON            NULL DEFAULT NULL COMMENT '任务参数 JSON',
    `description`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '任务描述',
    `is_system`        INT NOT NULL DEFAULT 0 COMMENT '0=自定义 1=系统内置（不可删除）',
    `run_type`         INT NOT NULL DEFAULT 1 COMMENT '1=定时自动 2=手动触发',
    `max_retries`      INT NOT NULL DEFAULT 0 COMMENT '失败后最多重试次数',
    `retry_delay_sec`  INT UNSIGNED    NOT NULL DEFAULT 60 COMMENT '重试间隔秒数',
    `timeout_sec`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '单次执行超时秒数，0=无限制',
    `last_run_at`      DATETIME        NULL DEFAULT NULL COMMENT '上次执行时间',
    `last_run_status`  INT NULL DEFAULT NULL COMMENT '上次执行状态：0=失败 1=成功',
    `next_run_at`      DATETIME        NULL DEFAULT NULL COMMENT '下次执行时间',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_st_task_group` (`task_group`),
    INDEX `idx_st_status` (`status`),
    INDEX `idx_st_next_run_at` (`next_run_at`),
    INDEX `idx_st_last_run_status` (`last_run_status`),
    UNIQUE KEY `uk_st_task_code` (`school_id`, `task_code`, `is_deleted_null`),
    CONSTRAINT `fk_st_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_st_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用定时任务调度表';


-- 5. audit_logs — 审核记录表
CREATE TABLE `audit_logs` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `auditable_type`   VARCHAR(100)    NOT NULL COMMENT '关联模型类型',
    `auditable_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联模型ID',
    `auditor_id`       BIGINT UNSIGNED NOT NULL COMMENT '审核人ID',
    `action`           INT NOT NULL COMMENT '1=通过 2=退回 3=撤回 4=转交',
    `comment`          TEXT            NULL DEFAULT NULL COMMENT '审核意见',
    `revoke_reason`    TEXT            NULL DEFAULT NULL COMMENT '撤销原因（action=3 时填写）',
    `revoked_log_id`   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '被撤销的审核记录 ID',
    `version`          INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '审核轮次',
    `is_deletable`     INT NOT NULL DEFAULT 0 COMMENT '0=禁止普通用户删除 1=允许删除',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_al_auditable` (`auditable_type`, `auditable_id`),
    INDEX `idx_al_auditor_id` (`auditor_id`),
    INDEX `idx_al_created_at` (`created_at`),
    INDEX `idx_al_revoked_log_id` (`revoked_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';


-- 6. system_logs — 系统操作日志表
CREATE TABLE `system_logs` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '业务关联用户ID',
    `operator_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '操作人ID',
    `role_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '操作人角色ID',
    `role_name`        VARCHAR(100)    NULL DEFAULT NULL COMMENT '操作人角色名称快照',
    `action`           VARCHAR(100)    NOT NULL COMMENT '操作类型',
    `module`           VARCHAR(100)    NOT NULL COMMENT '操作模块',
    `description`      TEXT            NULL DEFAULT NULL COMMENT '操作描述',
    `before_data`      JSON            NULL DEFAULT NULL COMMENT '修改前数据快照',
    `after_data`       JSON            NULL DEFAULT NULL COMMENT '修改后数据快照',
    `log_level`        INT NOT NULL DEFAULT 1 COMMENT '1=普通日志 2=用户动态 3=审计日志',
    `is_deletable`     INT NOT NULL DEFAULT 0 COMMENT '0=禁止普通用户删除 1=允许删除',
    `is_display`       INT NOT NULL DEFAULT 0 COMMENT '0=仅后台 1=前端展示',
    `activity_name`    VARCHAR(255)    NULL DEFAULT NULL COMMENT '动态展示名称',
    `status` INT NULL DEFAULT NULL COMMENT '关联记录状态',
    `status_label`     VARCHAR(50)     NULL DEFAULT NULL COMMENT '状态显示文本',
    `related_type`     VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型类型',
    `related_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录ID',
    `scope_type`       INT NULL DEFAULT NULL COMMENT '操作范围类型：1=全校 2=学院 3=专业 4=班级',
    `ip_address`       VARCHAR(45)     NULL DEFAULT NULL COMMENT 'IP地址',
    `user_agent`       VARCHAR(500)    NULL DEFAULT NULL COMMENT '用户代理',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `retention_until`  DATETIME        NULL DEFAULT NULL COMMENT '数据保留截止时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sl_operator_module` (`operator_id`, `module`, `created_at`),
    INDEX `idx_sl_user_time` (`user_id`, `created_at`),
    INDEX `idx_sl_role_time` (`role_id`, `created_at`),
    INDEX `idx_sl_module_action_time` (`module`, `action`, `created_at`),
    INDEX `idx_sl_level_display` (`log_level`, `is_display`),
    INDEX `idx_sl_related` (`related_type`, `related_id`),
    INDEX `idx_sl_scope_type` (`scope_type`),
    INDEX `idx_sl_retention_until` (`retention_until`),
    CONSTRAINT `ck_sl_user_or_operator` CHECK (`user_id` IS NOT NULL OR `operator_id` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';


-- 7. login_logs — 登录日志/安全审计表
CREATE TABLE `login_logs` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 users.id',
    `login_type`       INT NOT NULL DEFAULT 1 COMMENT '1=密码登录 2=扫码登录 3=SSO登录',
    `ip_address`       VARCHAR(45)     NULL DEFAULT NULL COMMENT 'IP地址',
    `user_agent`       VARCHAR(500)    NULL DEFAULT NULL COMMENT '用户代理',
    `login_status`     INT NOT NULL DEFAULT 1 COMMENT '0=失败 1=成功',
    `fail_reason`      VARCHAR(100)    NULL DEFAULT NULL COMMENT '失败原因',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `retention_until`  DATETIME        NULL DEFAULT NULL COMMENT '数据保留截止时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ll_user_time` (`user_id`, `created_at`),
    INDEX `idx_ll_school_status_time` (`school_id`, `login_status`, `created_at`),
    INDEX `idx_ll_created_at` (`created_at`),
    INDEX `idx_ll_retention_until` (`retention_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志/安全审计表';


-- 8. user_import_logs — 用户导入日志表
CREATE TABLE `user_import_logs` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`      BIGINT UNSIGNED NOT NULL COMMENT '目标学校ID',
    `operator_id`    BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    `file_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联导入文件',
    `total_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    `success_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    `fail_details`   JSON            NULL DEFAULT NULL COMMENT '失败详情',
    `import_status`  INT NOT NULL DEFAULT 0 COMMENT '0=导入中 1=完成 2=失败',
    `started_at`     DATETIME        NULL DEFAULT NULL COMMENT '开始时间',
    `completed_at`   DATETIME        NULL DEFAULT NULL COMMENT '完成时间',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_uil_school_id` (`school_id`),
    INDEX `idx_uil_operator_id` (`operator_id`),
    INDEX `idx_uil_import_status` (`import_status`),
    INDEX `idx_uil_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户导入日志表';


-- 9. user_behavior_logs — 用户行为记录表
CREATE TABLE `user_behavior_logs` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `behavior_type`    VARCHAR(50)     NOT NULL COMMENT '行为类型：click/navigate/search',
    `target_page`      VARCHAR(255)    NOT NULL COMMENT '目标页面',
    `target_module`    VARCHAR(100)    NULL DEFAULT NULL COMMENT '目标模块',
    `meta`             JSON            NULL DEFAULT NULL COMMENT '附加信息',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `retention_until`  DATETIME        NULL DEFAULT NULL COMMENT '数据保留截止时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ubl_user_page_time` (`user_id`, `target_page`, `created_at`),
    INDEX `idx_ubl_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为记录表';
