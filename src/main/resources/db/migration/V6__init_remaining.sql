-- ============================================================================
-- V6: 职业规划、短板识别、成长时间轴、审批流程、日志消息等
-- (Career, Weakness, Growth, Approval, Logs, Messages, AI, etc.)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 6.1 短板识别表
-- ----------------------------------------------------------------------------
CREATE TABLE weakness_analyses (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    weakness_type   VARCHAR(100)    NOT NULL COMMENT '短板类型',
    weakness_desc   TEXT            NULL DEFAULT NULL COMMENT '短板描述',
    source          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=AI生成 2=教师建议',
    teacher_id      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '教师ID（source=2时必填）',
    severity_level  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '严重程度 1-5',
    target_score    INT UNSIGNED    NULL DEFAULT NULL COMMENT '目标分数（距目标X分）',
    is_read         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_weakness_analyses_user (user_id),
    INDEX idx_weakness_analyses_source (source),
    INDEX idx_weakness_analyses_teacher (teacher_id),
    CONSTRAINT chk_weakness_severity CHECK (severity_level BETWEEN 1 AND 5),
    CONSTRAINT fk_weakness_analyses_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_weakness_analyses_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短板识别表';

-- ----------------------------------------------------------------------------
-- 6.2 改进建议表
-- ----------------------------------------------------------------------------
CREATE TABLE improvement_suggestions (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    weakness_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 weakness_analyses.id',
    suggestion_content  TEXT            NOT NULL COMMENT '建议内容',
    source              TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=AI生成 2=教师建议',
    teacher_id          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '教师ID（source=2时必填）',
    is_implemented      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=未落实 1=已落实',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_improvement_suggestions_weakness (weakness_id),
    INDEX idx_improvement_suggestions_source (source),
    INDEX idx_improvement_suggestions_teacher (teacher_id),
    CONSTRAINT fk_improvement_suggestions_weakness FOREIGN KEY (weakness_id) REFERENCES weakness_analyses(id),
    CONSTRAINT fk_improvement_suggestions_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='改进建议表';

-- ----------------------------------------------------------------------------
-- 6.3 短板改进进度跟踪表
-- ----------------------------------------------------------------------------
CREATE TABLE weakness_progress (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    weakness_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 weakness_analyses.id',
    progress_value  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '进度值 0-100',
    progress_desc   VARCHAR(255)    NULL DEFAULT NULL COMMENT '进度描述',
    recorded_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_weakness_progress_weakness (weakness_id),
    INDEX idx_weakness_progress_recorded (recorded_at),
    CONSTRAINT chk_weakness_progress_value CHECK (progress_value <= 100),
    CONSTRAINT fk_weakness_progress_weakness FOREIGN KEY (weakness_id) REFERENCES weakness_analyses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短板改进进度跟踪表';

-- ----------------------------------------------------------------------------
-- 6.4 职业规划表
-- ----------------------------------------------------------------------------
CREATE TABLE career_plans (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    current_version  INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    submit_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '提交次数',
    semester_id      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    title            VARCHAR(255)    NOT NULL COMMENT '规划标题',
    content          TEXT            NULL DEFAULT NULL COMMENT '规划内容正文',
    requirement      TEXT            NULL DEFAULT NULL COMMENT '要求/目标',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审批 2=通过 3=驳回 4=需修改',
    submitted_at     DATETIME        NULL DEFAULT NULL COMMENT '提交时间',
    audited_at       DATETIME        NULL DEFAULT NULL COMMENT '审核时间',
    auditor_id       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
    rejected_reason  TEXT            NULL DEFAULT NULL COMMENT '驳回原因',
    file_id          BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联文件ID',
    draft_saved_at   TIMESTAMP       NULL DEFAULT NULL COMMENT '草稿自动保存时间',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_career_plans_user_semester (user_id, semester_id),
    INDEX idx_career_plans_status (status),
    INDEX idx_career_plans_school_id (school_id),
    CONSTRAINT fk_career_plans_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_career_plans_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_career_plans_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_career_plans_auditor FOREIGN KEY (auditor_id) REFERENCES users(id),
    CONSTRAINT fk_career_plans_file FOREIGN KEY (file_id) REFERENCES file_uploads(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业规划表';

-- ----------------------------------------------------------------------------
-- 6.5 职业规划版本历史表
-- ----------------------------------------------------------------------------
CREATE TABLE career_plan_versions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    career_plan_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 career_plans.id',
    version         INT UNSIGNED    NOT NULL COMMENT '版本号',
    title           VARCHAR(255)    NOT NULL COMMENT '规划标题',
    data_snapshot   JSON            NOT NULL COMMENT '完整数据快照',
    status          TINYINT UNSIGNED NOT NULL COMMENT '该版本状态',
    rejected_reason TEXT            NULL DEFAULT NULL COMMENT '驳回原因',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_career_plan_versions (career_plan_id, version),
    INDEX idx_career_plan_versions_plan (career_plan_id),
    CONSTRAINT fk_career_plan_versions_plan FOREIGN KEY (career_plan_id) REFERENCES career_plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业规划版本历史表';

-- ----------------------------------------------------------------------------
-- 6.6 成长时间轴表
-- ----------------------------------------------------------------------------
CREATE TABLE growth_timelines (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    semester_id   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    event_type    TINYINT         NOT NULL COMMENT '1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升',
    event_name    VARCHAR(255)    NOT NULL COMMENT '事件名称',
    content       TEXT            NULL DEFAULT NULL COMMENT '事件详细描述/富文本',
    cover_image   VARCHAR(500)    NULL DEFAULT NULL COMMENT '封面图片URL',
    ability_data  JSON            NULL DEFAULT NULL COMMENT '能力维度数据',
    tags          JSON            NULL DEFAULT NULL COMMENT '标签数组',
    event_time    DATE            NOT NULL COMMENT '发生时间',
    source_id     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '来源记录ID',
    source_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '来源模型类型',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_growth_timelines_user_time (user_id, event_time),
    INDEX idx_growth_timelines_user_type (user_id, event_type),
    INDEX idx_growth_timelines_source (source_type, source_id),
    INDEX idx_growth_timelines_school_id (school_id),
    CONSTRAINT fk_growth_timelines_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_growth_timelines_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长时间轴表';

-- ----------------------------------------------------------------------------
-- 6.7 审批流程配置表
-- ----------------------------------------------------------------------------
CREATE TABLE approval_flows (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id           BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id（支持多校）',
    flow_name           VARCHAR(100)    NOT NULL COMMENT '流程名称',
    applicable_type     VARCHAR(50)     NOT NULL COMMENT '适用类型：Archive/AwardApplication/CareerPlan',
    applicable_sub_type VARCHAR(50)     NULL DEFAULT NULL COMMENT '适用子类型',
    version             INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '流程版本号',
    copied_from         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '复制自哪个流程ID',
    is_default          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认流程',
    status              TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_by          BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_flows (school_id, applicable_type, applicable_sub_type, version),
    INDEX idx_approval_flows_default (is_default, status),
    INDEX idx_approval_flows_copied (copied_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程配置表';

-- ----------------------------------------------------------------------------
-- 6.8 审批流程步骤表
-- ----------------------------------------------------------------------------
CREATE TABLE approval_flow_steps (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    flow_id             BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_flows.id',
    step_no             TINYINT UNSIGNED NOT NULL COMMENT '第几步',
    step_name           VARCHAR(100)    NOT NULL COMMENT '步骤名称',
    role_id             BIGINT UNSIGNED NOT NULL COMMENT '该节点要求的角色',
    scope_type          TINYINT UNSIGNED NOT NULL COMMENT '范围类型：1=学校 2=学院 3=专业 4=班级',
    scope_rule          VARCHAR(50)     NOT NULL COMMENT '范围规则：student_class/student_major等',
    auto_assign         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=自动分配 0=手动',
    allow_delegate      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许委托',
    allow_skip          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许跳过',
    allow_designate_next TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1=允许指定下一审批人',
    timeout_hours       INT UNSIGNED    NOT NULL DEFAULT 48 COMMENT '超时小时数',
    reject_action       VARCHAR(20)     NOT NULL DEFAULT 'end' COMMENT '驳回动作：end/return',
    reject_to_step      TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '驳回后退回步骤',
    sort                INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_flow_steps (flow_id, step_no),
    INDEX idx_approval_flow_steps_flow (flow_id),
    INDEX idx_approval_flow_steps_role (role_id),
    CONSTRAINT fk_approval_flow_steps_flow FOREIGN KEY (flow_id) REFERENCES approval_flows(id),
    CONSTRAINT fk_approval_flow_steps_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程步骤表';

-- ----------------------------------------------------------------------------
-- 6.9 审批流程实例表
-- ----------------------------------------------------------------------------
CREATE TABLE approval_instances (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    approvable_type  VARCHAR(100)    NOT NULL COMMENT '模型类型：Archive/AwardApplication/CareerPlan',
    approvable_id    BIGINT UNSIGNED NOT NULL COMMENT '模型ID',
    flow_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_flows.id',
    flow_version     INT UNSIGNED    NOT NULL COMMENT '实例创建时使用的流程版本',
    applicant_id     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    current_step     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '当前执行到第几步',
    total_steps      TINYINT UNSIGNED NOT NULL COMMENT '总步数',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=审批中 2=已通过 3=已驳回 4=已撤回 5=需修改',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP       NULL DEFAULT NULL COMMENT '完成时间',
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_instances (approvable_type, approvable_id),
    INDEX idx_approval_instances_flow (flow_id, flow_version),
    INDEX idx_approval_instances_status (status),
    INDEX idx_approval_instances_applicant (applicant_id),
    CONSTRAINT fk_approval_instances_flow FOREIGN KEY (flow_id) REFERENCES approval_flows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流程实例表';

-- ----------------------------------------------------------------------------
-- 6.10 审批委托表
-- ----------------------------------------------------------------------------
CREATE TABLE approval_delegations (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    delegator_id   BIGINT UNSIGNED NOT NULL COMMENT '委托人ID',
    delegatee_id   BIGINT UNSIGNED NOT NULL COMMENT '受托人ID',
    role_id        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '委托的角色范围',
    scope_type     TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '委托的范围类型',
    scope_id       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '委托的范围ID',
    flow_id        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '指定委托某个流程（null=全部）',
    start_time     DATETIME        NOT NULL COMMENT '委托开始时间',
    end_time       DATETIME        NOT NULL COMMENT '委托结束时间',
    reason         VARCHAR(255)    NULL DEFAULT NULL COMMENT '委托原因',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=已取消 1=生效中 2=已过期',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_approval_delegations_delegator (school_id, delegator_id, status),
    INDEX idx_approval_delegations_delegatee (delegatee_id, status),
    INDEX idx_approval_delegations_role (role_id, scope_type, scope_id),
    CONSTRAINT fk_approval_delegations_delegator FOREIGN KEY (delegator_id) REFERENCES users(id),
    CONSTRAINT fk_approval_delegations_delegatee FOREIGN KEY (delegatee_id) REFERENCES users(id),
    CONSTRAINT fk_approval_delegations_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批委托表';

-- ----------------------------------------------------------------------------
-- 6.11 审批节点记录表
-- ----------------------------------------------------------------------------
CREATE TABLE approval_nodes (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    instance_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_instances.id',
    step_no             TINYINT UNSIGNED NOT NULL COMMENT '第几步',
    step_name           VARCHAR(100)    NOT NULL COMMENT '步骤名称（从 flow 复制）',
    role_id             BIGINT UNSIGNED NOT NULL COMMENT '该节点要求的角色',
    scope_type          TINYINT UNSIGNED NOT NULL COMMENT '范围类型',
    scope_id            BIGINT UNSIGNED NOT NULL COMMENT '对应组织ID',
    assigned_auditor_id BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '分配的审批人（null=待分配）',
    assign_type         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=自动分配 2=手动指定 3=系统指定 4=上级指定',
    actual_auditor_id   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '实际审批人（委托场景）',
    delegation_id       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 approval_delegations.id',
    action              TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '1=通过 2=驳回 3=转交 4=跳过',
    comment             TEXT            NULL DEFAULT NULL COMMENT '审批意见',
    next_node_id        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '动态指定的下一节点ID',
    started_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '节点开始时间',
    completed_at        TIMESTAMP       NULL DEFAULT NULL COMMENT '节点完成时间',
    timeout_at          TIMESTAMP       NULL DEFAULT NULL COMMENT '超时提醒时间',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_nodes (instance_id, step_no),
    INDEX idx_approval_nodes_assigned (assigned_auditor_id, completed_at),
    INDEX idx_approval_nodes_actual (actual_auditor_id),
    INDEX idx_approval_nodes_role_scope (role_id, scope_type, scope_id),
    INDEX idx_approval_nodes_next (next_node_id),
    CONSTRAINT fk_approval_nodes_instance FOREIGN KEY (instance_id) REFERENCES approval_instances(id),
    CONSTRAINT fk_approval_nodes_delegation FOREIGN KEY (delegation_id) REFERENCES approval_delegations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批节点记录表';

-- ----------------------------------------------------------------------------
-- 6.12 统一待审批任务表
-- ----------------------------------------------------------------------------
CREATE TABLE pending_approvals (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    node_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_nodes.id',
    instance_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 approval_instances.id',
    approvable_type  VARCHAR(100)    NOT NULL COMMENT '模型类型',
    approvable_id    BIGINT UNSIGNED NOT NULL COMMENT '模型ID',
    applicant_id     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    applicant_name   VARCHAR(100)    NOT NULL COMMENT '申请人姓名',
    applicant_no     VARCHAR(50)     NOT NULL COMMENT '申请人学号/工号',
    title            VARCHAR(255)    NOT NULL COMMENT '申请标题',
    content          TEXT            NULL DEFAULT NULL COMMENT '申报内容摘要',
    category_label   VARCHAR(50)     NOT NULL COMMENT '分类标签',
    submit_time      DATETIME        NOT NULL COMMENT '提交时间',
    auditor_id       BIGINT UNSIGNED NOT NULL COMMENT '指定审批人ID',
    role_id          BIGINT UNSIGNED NOT NULL COMMENT '当前审批节点角色',
    step_no          TINYINT UNSIGNED NOT NULL COMMENT '当前第几步',
    step_name        VARCHAR(100)    NOT NULL COMMENT '步骤名称',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=待审批 2=审批中 3=已委托',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_approvals_node (node_id),
    INDEX idx_pending_approvals_approvable (approvable_type, approvable_id),
    INDEX idx_pending_approvals_auditor (auditor_id, status),
    INDEX idx_pending_approvals_applicant (applicant_id),
    INDEX idx_pending_approvals_role (role_id),
    CONSTRAINT fk_pending_approvals_node FOREIGN KEY (node_id) REFERENCES approval_nodes(id),
    CONSTRAINT fk_pending_approvals_instance FOREIGN KEY (instance_id) REFERENCES approval_instances(id),
    CONSTRAINT fk_pending_approvals_auditor FOREIGN KEY (auditor_id) REFERENCES users(id),
    CONSTRAINT fk_pending_approvals_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_pending_approvals_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一待审批任务表';

-- ----------------------------------------------------------------------------
-- 6.13 审核记录表
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    auditable_type  VARCHAR(100)    NOT NULL COMMENT '关联模型类型',
    auditable_id    BIGINT UNSIGNED NOT NULL COMMENT '关联模型ID',
    auditor_id      BIGINT UNSIGNED NOT NULL COMMENT '审核人ID',
    action          TINYINT UNSIGNED NOT NULL COMMENT '1=通过 2=驳回 3=撤回 4=转交',
    comment         TEXT            NULL DEFAULT NULL COMMENT '审核意见',
    version         INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '审核轮次',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_logs_auditable (auditable_type, auditable_id),
    INDEX idx_audit_logs_auditor (auditor_id),
    INDEX idx_audit_logs_created (created_at),
    CONSTRAINT fk_audit_logs_auditor FOREIGN KEY (auditor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- ----------------------------------------------------------------------------
-- 6.14 系统操作日志表
-- ----------------------------------------------------------------------------
CREATE TABLE system_logs (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '业务关联用户ID（学生/被操作对象）',
    operator_id    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '操作人ID（执行操作的人）',
    action         VARCHAR(100)    NOT NULL COMMENT '操作类型',
    module         VARCHAR(100)    NOT NULL COMMENT '操作模块',
    description    TEXT            NULL DEFAULT NULL COMMENT '操作描述',
    log_level      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=普通日志 2=用户动态 3=审计日志',
    is_display     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=仅后台 1=前端展示',
    activity_name  VARCHAR(255)    NULL DEFAULT NULL COMMENT '动态展示名称',
    status         TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录状态',
    status_label   VARCHAR(50)     NULL DEFAULT NULL COMMENT '状态显示文本',
    related_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型类型',
    related_id     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录ID',
    ip_address     VARCHAR(45)     NULL DEFAULT NULL COMMENT 'IP地址',
    user_agent     VARCHAR(500)    NULL DEFAULT NULL COMMENT '用户代理',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_system_logs_user_id (user_id),
    INDEX idx_system_logs_module (module),
    INDEX idx_system_logs_created (created_at),
    INDEX idx_system_logs_level_display (log_level, is_display),
    INDEX idx_system_logs_related (related_type, related_id),
    INDEX idx_system_logs_operator (operator_id),
    CONSTRAINT fk_system_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_system_logs_operator FOREIGN KEY (operator_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- ----------------------------------------------------------------------------
-- 6.15 AI生成记录表
-- ----------------------------------------------------------------------------
CREATE TABLE ai_generation_logs (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id            BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    generation_type    VARCHAR(50)     NOT NULL COMMENT '生成类型',
    input_data         JSON            NOT NULL COMMENT '输入数据摘要（脱敏）',
    output_content     TEXT            NOT NULL COMMENT 'AI生成的内容',
    model_name         VARCHAR(100)    NULL DEFAULT NULL COMMENT '使用的AI模型',
    token_usage        INT UNSIGNED    NULL DEFAULT NULL COMMENT 'Token消耗量',
    generation_time_ms INT UNSIGNED    NULL DEFAULT NULL COMMENT '生成耗时（毫秒）',
    is_used            TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=未采用 1=已采用',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_generation_logs_user (user_id),
    INDEX idx_ai_generation_logs_type (generation_type),
    INDEX idx_ai_generation_logs_model (model_name),
    CONSTRAINT fk_ai_generation_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成记录表';

-- ----------------------------------------------------------------------------
-- 6.16 公告/信息发布表
-- ----------------------------------------------------------------------------
CREATE TABLE announcements (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title         VARCHAR(255)    NOT NULL COMMENT '标题',
    content       TEXT            NOT NULL COMMENT '内容',
    publisher_id  BIGINT UNSIGNED NOT NULL COMMENT '发布人ID',
    target_type   VARCHAR(50)     NOT NULL COMMENT '发布对象：all/college/major/class',
    target_id     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '目标对象ID',
    publish_time  DATETIME        NOT NULL COMMENT '发布时间',
    status        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_announcements_publisher (publisher_id),
    INDEX idx_announcements_target (target_type, target_id),
    INDEX idx_announcements_status (status),
    CONSTRAINT fk_announcements_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告/信息发布表';

-- ----------------------------------------------------------------------------
-- 6.17 公告已读记录表
-- ----------------------------------------------------------------------------
CREATE TABLE announcement_reads (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    announcement_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 announcements.id',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    read_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_announcement_reads (announcement_id, user_id),
    INDEX idx_announcement_reads_user (user_id),
    CONSTRAINT fk_announcement_reads_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id),
    CONSTRAINT fk_announcement_reads_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';

-- ----------------------------------------------------------------------------
-- 6.18 用户消息中心表
-- ----------------------------------------------------------------------------
CREATE TABLE user_messages (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
    sender_id     BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '发送人ID（系统消息为NULL）',
    sender_type   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=系统 2=人工 3=自动触发',
    template_id   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 message_templates.id',
    category      VARCHAR(50)     NOT NULL COMMENT '消息分类：审批提醒/系统通知/动态提醒',
    title         VARCHAR(255)    NOT NULL COMMENT '消息标题',
    content       TEXT            NULL DEFAULT NULL COMMENT '消息内容',
    related_type  VARCHAR(100)    NULL DEFAULT NULL COMMENT '关联模型类型',
    related_id    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联记录ID',
    send_channel  VARCHAR(20)     NOT NULL DEFAULT 'push' COMMENT '发送渠道：push/email/sms',
    is_read       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    read_at       TIMESTAMP       NULL DEFAULT NULL COMMENT '阅读时间',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_messages_user_read (user_id, is_read, deleted_at),
    INDEX idx_user_messages_category (category),
    INDEX idx_user_messages_related (related_type, related_id),
    INDEX idx_user_messages_created (created_at),
    CONSTRAINT fk_user_messages_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_user_messages_template FOREIGN KEY (template_id) REFERENCES message_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息中心表';

-- ----------------------------------------------------------------------------
-- 6.19 AI 对话会话表
-- ----------------------------------------------------------------------------
CREATE TABLE ai_conversations (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id   BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    title       VARCHAR(255)    NULL DEFAULT NULL COMMENT '会话标题（可自动生成）',
    context     JSON            NULL DEFAULT NULL COMMENT '系统上下文/人设配置',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后对话时间',
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_conversations_user_status (user_id, status),
    INDEX idx_ai_conversations_user_updated (user_id, updated_at),
    INDEX idx_ai_conversations_school (school_id),
    CONSTRAINT fk_ai_conversations_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- ----------------------------------------------------------------------------
-- 6.20 AI 对话消息表
-- ----------------------------------------------------------------------------
CREATE TABLE ai_messages (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id    BIGINT UNSIGNED NOT NULL COMMENT '关联会话',
    role               VARCHAR(20)     NOT NULL COMMENT 'user / assistant / system',
    content            TEXT            NOT NULL COMMENT '消息内容',
    model_name         VARCHAR(100)    NULL DEFAULT NULL COMMENT '使用的AI模型',
    token_usage        INT UNSIGNED    NULL DEFAULT NULL COMMENT 'Token消耗量',
    generation_time_ms INT UNSIGNED    NULL DEFAULT NULL COMMENT '生成耗时（毫秒）',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_messages_conversation (conversation_id, created_at),
    CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';

-- ----------------------------------------------------------------------------
-- 6.21 用户行为记录表
-- ----------------------------------------------------------------------------
CREATE TABLE user_behavior_logs (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    behavior_type  VARCHAR(50)     NOT NULL COMMENT '行为类型：click/navigate/search',
    target_page    VARCHAR(255)    NULL DEFAULT NULL COMMENT '目标页面',
    target_module  VARCHAR(100)    NULL DEFAULT NULL COMMENT '目标模块',
    meta           JSON            NULL DEFAULT NULL COMMENT '附加信息',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_behavior_logs_user_page_time (user_id, target_page, created_at),
    INDEX idx_user_behavior_logs_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为记录表';

-- ----------------------------------------------------------------------------
-- 6.22 安全审计表 - 登录记录
-- ----------------------------------------------------------------------------
CREATE TABLE login_logs (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 users.id',
    login_type    TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=密码登录 2=扫码登录 3=SSO登录',
    ip_address    VARCHAR(45)     NULL DEFAULT NULL COMMENT 'IP地址',
    user_agent    VARCHAR(500)    NULL DEFAULT NULL COMMENT '用户代理',
    login_status  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=失败 1=成功',
    fail_reason   VARCHAR(100)    NULL DEFAULT NULL COMMENT '失败原因',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_login_logs_user_created (user_id, created_at),
    INDEX idx_login_logs_school (school_id),
    INDEX idx_login_logs_created (created_at),
    CONSTRAINT fk_login_logs_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_login_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审计表（登录记录）';
