-- ============================================================
-- V3: Data Security and Approval Workflow
-- 数据安全策略、脱敏规则、审批流程全套表
-- ============================================================

-- 1. data_permission_rules — 数据权限规则表
CREATE TABLE `data_permission_rules` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `role_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 roles.id',
    `rule_name`        VARCHAR(100)    NOT NULL COMMENT '规则名称',
    `rule_code`        VARCHAR(50)     NOT NULL COMMENT '规则编码（全局唯一）',
    `target_table`     VARCHAR(100)    NOT NULL COMMENT '目标数据表/资源',
    `target_fields`    JSON            NULL DEFAULT NULL COMMENT '可访问字段白名单，NULL=全部',
    `filter_rule`      JSON            NULL DEFAULT NULL COMMENT '行级过滤规则 JSON',
    `scope_relation`   VARCHAR(20)     NOT NULL DEFAULT 'AND' COMMENT '与role_scopes的关系：AND/OR/OVERRIDE',
    `priority`         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '优先级',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_dpr_school_id` (`school_id`),
    INDEX `idx_dpr_role_status` (`role_id`, `status`),
    INDEX `idx_dpr_status` (`status`),
    UNIQUE KEY `uk_dpr_rule_code` (`rule_code`, `is_deleted_null`),
    CONSTRAINT `fk_dpr_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dpr_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_dpr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限规则表';


-- 2. data_masking_rules — 数据脱敏规则配置表
CREATE TABLE `data_masking_rules` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 schools.id，NULL=系统全局规则',
    `rule_name`        VARCHAR(100)    NOT NULL COMMENT '规则名称',
    `rule_code`        VARCHAR(50)     NOT NULL COMMENT '规则编码（全局唯一）',
    `target_table`     VARCHAR(100)    NOT NULL COMMENT '目标表',
    `target_field`     VARCHAR(100)    NOT NULL COMMENT '目标字段',
    `masking_strategy` VARCHAR(50)     NOT NULL COMMENT '脱敏策略编码',
    `masking_param`    JSON            NULL DEFAULT NULL COMMENT '策略参数',
    `apply_scenarios`  JSON            NULL DEFAULT NULL COMMENT '适用场景：["export","api","screen","log"]',
    `priority`         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '优先级',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dmr_rule_code` (`rule_code`, `is_deleted_null`),
    UNIQUE KEY `uk_dmr_school_field` (`school_id`, `target_table`, `target_field`, `is_deleted_null`),
    INDEX `idx_dmr_school_status` (`school_id`, `status`),
    INDEX `idx_dmr_target` (`target_table`, `target_field`),
    CONSTRAINT `fk_dmr_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dmr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据脱敏规则配置表';


-- 3. data_security_policies — 数据安全策略配置表
CREATE TABLE `data_security_policies` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 schools.id，NULL=系统全局策略',
    `policy_type`      VARCHAR(50)     NOT NULL COMMENT '策略类型：password/session/backup/retention/login_lock/masking_default',
    `policy_name`      VARCHAR(100)    NOT NULL COMMENT '策略名称',
    `policy_config`    JSON            NOT NULL COMMENT '策略配置 JSON',
    `effective_at`     DATETIME        NULL DEFAULT NULL COMMENT '生效时间，NULL=立即生效',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dsp_school_type` (`school_id`, `policy_type`, `is_deleted_null`),
    INDEX `idx_dsp_school_status` (`school_id`, `status`),
    CONSTRAINT `fk_dsp_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dsp_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据安全策略配置表';


-- 4. approval_delegations — 审批委托记录表
CREATE TABLE `approval_delegations` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `delegator_id`     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '委托人ID（原审批人）',
    `delegatee_id`     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '受托人ID',
    `role_id`          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '委托角色范围（NULL=全部角色）',
    `scope_type`       TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '委托范围类型：2=学院 3=专业 4=班级（NULL=全部）',
    `scope_id`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '委托范围ID（NULL=全部）',
    `start_time`       DATETIME        NOT NULL COMMENT '委托开始时间',
    `end_time`         DATETIME        NOT NULL COMMENT '委托结束时间',
    `reason`           VARCHAR(255)    NULL DEFAULT NULL COMMENT '委托原因',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=待生效 1=生效中 2=已过期 3=已取消',
    `cancelled_at`     DATETIME        NULL DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`    VARCHAR(255)    NULL DEFAULT NULL COMMENT '取消原因',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ad_school_id` (`school_id`),
    INDEX `idx_ad_delegator_status` (`delegator_id`, `status`),
    INDEX `idx_ad_delegatee_status` (`delegatee_id`, `status`),
    INDEX `idx_ad_start_time` (`start_time`),
    INDEX `idx_ad_end_time` (`end_time`),
    INDEX `idx_ad_status` (`status`),
    UNIQUE KEY `uk_ad_delegation` (`delegator_id`, `delegatee_id`, `start_time`, `end_time`, `is_deleted_null`),
    CONSTRAINT `fk_ad_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ad_delegator_id` FOREIGN KEY (`delegator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ad_delegatee_id` FOREIGN KEY (`delegatee_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ad_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批委托记录表';


-- 5. approval_flows — 审批流程配置表
CREATE TABLE `approval_flows` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `flow_name`           VARCHAR(100)    NOT NULL COMMENT '流程名称',
    `applicable_type`     VARCHAR(50)     NOT NULL COMMENT '适用类型：Archive/AwardApplication/CareerPlan',
    `applicable_sub_type` VARCHAR(50)     NULL DEFAULT NULL COMMENT '适用子类型',
    `version`             INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '流程版本号',
    `copied_from`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '复制自哪个流程ID',
    `is_default`          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认流程',
    `status`              TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`          BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    `created_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`     TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_af_copied_from` (`copied_from`),
    INDEX `idx_af_default_status` (`is_default`, `status`),
    UNIQUE KEY `uk_af_version` (`school_id`, `applicable_type`, `applicable_sub_type`, `version`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程配置表';


-- 6. approval_flow_mappings — 审批流程业务映射表
CREATE TABLE `approval_flow_mappings` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `business_type`     VARCHAR(50)     NOT NULL COMMENT '业务类型：Archive/AwardApplication/CareerPlan/Announcement',
    `business_sub_type` VARCHAR(50)     NULL DEFAULT NULL COMMENT '业务子类型（NULL=通用）',
    `flow_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_flows.id',
    `is_default`        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认流程',
    `effective_start`   DATETIME        NULL DEFAULT NULL COMMENT '生效开始时间',
    `effective_end`     DATETIME        NULL DEFAULT NULL COMMENT '生效结束时间',
    `priority`          INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '优先级',
    `created_by`        BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_afm_business_type` (`business_type`),
    INDEX `idx_afm_business_sub_type` (`business_sub_type`),
    INDEX `idx_afm_flow_id` (`flow_id`),
    INDEX `idx_afm_school_business` (`school_id`, `business_type`, `business_sub_type`, `is_default`),
    INDEX `idx_afm_is_default` (`is_default`),
    UNIQUE KEY `uk_afm_mapping` (`school_id`, `business_type`, `business_sub_type`, `is_deleted_null`),
    CONSTRAINT `ck_afm_effective` CHECK (`effective_end` IS NULL OR `effective_start` IS NULL OR `effective_end` > `effective_start`),
    CONSTRAINT `fk_afm_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_afm_flow_id` FOREIGN KEY (`flow_id`) REFERENCES `approval_flows` (`id`),
    CONSTRAINT `fk_afm_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程业务映射表';


-- 7. approval_flow_steps — 审批流程步骤表
CREATE TABLE `approval_flow_steps` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `flow_id`            BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_flows.id',
    `step_no`            TINYINT UNSIGNED NOT NULL COMMENT '第几步',
    `step_name`          VARCHAR(100)    NOT NULL COMMENT '步骤名称',
    `role_id`            BIGINT UNSIGNED NOT NULL COMMENT '该节点要求的角色',
    `scope_type`         TINYINT UNSIGNED NOT NULL COMMENT '范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级',
    `scope_rule`         VARCHAR(50)     NOT NULL COMMENT '范围规则：student_class/student_major 等',
    `auto_assign`        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=自动分配 0=手动',
    `allow_delegate`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许委托',
    `allow_skip`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许跳过',
    `allow_designate_next` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许指定下一审批人',
    `timeout_hours`      INT UNSIGNED    NOT NULL DEFAULT 48 COMMENT '超时小时数',
    `reject_action`      VARCHAR(20)     NOT NULL DEFAULT 'end' COMMENT '退回动作：end/return',
    `reject_to_step`     TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '退回后退回步骤（reject_action=return 时生效）',
    `sort`               INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`         TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`    TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_afs_flow_id` (`flow_id`),
    INDEX `idx_afs_role_id` (`role_id`),
    UNIQUE KEY `uk_afs_step` (`flow_id`, `step_no`, `is_deleted_null`),
    CONSTRAINT `fk_afs_flow_id` FOREIGN KEY (`flow_id`) REFERENCES `approval_flows` (`id`),
    CONSTRAINT `fk_afs_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程步骤表';


-- 8. approval_instances — 审批流程实例表
CREATE TABLE `approval_instances` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `approvable_type`  VARCHAR(100)    NOT NULL COMMENT '模型类型：Archive/AwardApplication/CareerPlan',
    `approvable_id`    BIGINT UNSIGNED NOT NULL COMMENT '模型ID',
    `flow_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_flows.id',
    `flow_version`     INT UNSIGNED    NOT NULL COMMENT '实例创建时使用的流程版本',
    `applicant_id`     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    `current_step`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '当前执行到第几步',
    `total_steps`      TINYINT UNSIGNED NOT NULL COMMENT '总步数',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=审批中 2=已通过 3=已退回 4=已撤回',
    `completed_at`     DATETIME        NULL DEFAULT NULL COMMENT '完成时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ai_flow` (`flow_id`, `flow_version`),
    INDEX `idx_ai_approvable` (`approvable_type`, `approvable_id`, `status`),
    INDEX `idx_ai_status` (`status`),
    INDEX `idx_ai_applicant_id` (`applicant_id`),
    UNIQUE KEY `uk_ai_approvable` (`approvable_type`, `approvable_id`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程实例表';


-- 9. approval_nodes — 审批节点记录表
CREATE TABLE `approval_nodes` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `instance_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_instances.id',
    `step_no`             TINYINT UNSIGNED NOT NULL COMMENT '第几步',
    `step_name`           VARCHAR(100)    NOT NULL COMMENT '步骤名称',
    `role_id`             BIGINT UNSIGNED NOT NULL COMMENT '该节点要求的角色',
    `scope_type`          TINYINT UNSIGNED NOT NULL COMMENT '范围类型',
    `scope_id`            BIGINT UNSIGNED NOT NULL COMMENT '对应组织ID',
    `assigned_auditor_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '分配的审批人（null=待分配）',
    `assign_type`         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=自动分配 2=手动指定 3=系统指定 4=上级指定',
    `actual_auditor_id`   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '实际审批人（委托场景=受托人）',
    `delegation_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 approval_delegations.id',
    `action`              TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '审核动作：NULL=待审核 1=通过 2=退回 3=转交 4=跳过',
    `comment`             TEXT            NULL DEFAULT NULL COMMENT '审批意见',
    `next_node_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '动态指定的下一节点ID',
    `started_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '节点开始时间',
    `completed_at`        DATETIME        NULL DEFAULT NULL COMMENT '节点完成时间',
    `timeout_at`          DATETIME        NULL DEFAULT NULL COMMENT '超时提醒时间',
    `created_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`     TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_an_assigned_auditor` (`assigned_auditor_id`, `completed_at`),
    INDEX `idx_an_actual_auditor` (`actual_auditor_id`),
    INDEX `idx_an_role_scope` (`role_id`, `scope_type`, `scope_id`),
    INDEX `idx_an_next_node` (`next_node_id`),
    UNIQUE KEY `uk_an_node` (`instance_id`, `step_no`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批节点记录表';


-- 10. pending_approvals — 统一待审批任务表
CREATE TABLE `pending_approvals` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `node_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_nodes.id',
    `instance_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_instances.id',
    `approvable_type`  VARCHAR(100)    NOT NULL COMMENT '模型类型',
    `approvable_id`    BIGINT UNSIGNED NOT NULL COMMENT '模型ID',
    `applicant_id`     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    `applicant_name`   VARCHAR(100)    NOT NULL COMMENT '快照：申请人姓名',
    `applicant_no`     VARCHAR(50)     NOT NULL COMMENT '快照：申请人学号/工号',
    `title`            VARCHAR(255)    NOT NULL COMMENT '申请标题',
    `content`          TEXT            NULL DEFAULT NULL COMMENT '申报内容摘要',
    `category_label`   VARCHAR(50)     NOT NULL COMMENT '分类标签',
    `submitted_at`     DATETIME        NOT NULL COMMENT '提交时间',
    `auditor_id`       BIGINT UNSIGNED NOT NULL COMMENT '指定审批人ID',
    `role_id`          BIGINT UNSIGNED NOT NULL COMMENT '当前审批节点角色',
    `step_no`          TINYINT UNSIGNED NOT NULL COMMENT '当前第几步',
    `step_name`        VARCHAR(100)    NOT NULL COMMENT '步骤名称',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=待审批 2=审批中 3=已委托',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_pa_approvable` (`approvable_type`, `approvable_id`),
    INDEX `idx_pa_auditor_status` (`auditor_id`, `status`),
    INDEX `idx_pa_applicant_id` (`applicant_id`),
    INDEX `idx_pa_role_id` (`role_id`),
    INDEX `idx_pa_auditor_status_time` (`auditor_id`, `status`, `submitted_at`),
    CONSTRAINT `fk_pa_node_id` FOREIGN KEY (`node_id`) REFERENCES `approval_nodes` (`id`),
    CONSTRAINT `fk_pa_auditor_id` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_pa_applicant_id` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_pa_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一待审批任务表';
