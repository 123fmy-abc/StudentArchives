-- ============================================================================
-- V3: 用户关联与档案扩展表（User Relations & Profiles）
-- 描述：用户角色关联、权限关联、角色范围绑定、学生/教师档案、兴趣、奖项汇总等
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 用户角色关联表
-- ----------------------------------------------------------------------------
CREATE TABLE user_roles (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_roles (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------------------------------------------------------
-- 3.2 角色权限关联表
-- ----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    permission_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 permissions.id',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permissions (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ----------------------------------------------------------------------------
-- 3.3 角色组织范围绑定表
-- ----------------------------------------------------------------------------
CREATE TABLE role_scopes (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    role_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    scope_type      TINYINT UNSIGNED NOT NULL COMMENT '范围类型：1=学校 2=学院 3=专业 4=班级',
    scope_id        BIGINT UNSIGNED NOT NULL COMMENT '对应组织表ID',
    is_primary      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=兼任 1=主职',
    appoint_by      BIGINT UNSIGNED NOT NULL COMMENT '任命人ID（系统管理员）',
    appoint_reason  VARCHAR(255)    NULL DEFAULT NULL COMMENT '任命原因/备注',
    valid_from      DATE            NOT NULL COMMENT '生效日期',
    valid_until     DATE            NULL DEFAULT NULL COMMENT '失效日期（空=永久）',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用 2=过期',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_scopes (user_id, role_id, scope_type, scope_id),
    INDEX idx_role_scopes_scope (scope_type, scope_id, status, valid_from, valid_until),
    INDEX idx_role_scopes_user_status (user_id, status),
    INDEX idx_role_scopes_role_scope (role_id, scope_type, scope_id, is_primary, status),
    CONSTRAINT fk_role_scopes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_scopes_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_scopes_appoint_by FOREIGN KEY (appoint_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色组织范围绑定表';

-- ----------------------------------------------------------------------------
-- 3.4 学生档案扩展表
-- ----------------------------------------------------------------------------
CREATE TABLE student_profiles (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    class_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 classes.id',
    political_status  VARCHAR(50)     NULL DEFAULT NULL COMMENT '政治面貌',
    volunteer_hours   DECIMAL(8,2)    NULL DEFAULT 0.00 COMMENT '志愿时长',
    language_ability  VARCHAR(255)    NULL DEFAULT NULL COMMENT '语言能力，如英语（CET-6）、日语（N3）',
    hobbies           VARCHAR(255)    NULL DEFAULT NULL COMMENT '运动爱好/特长，如篮球、跑步',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_profiles_user (user_id),
    INDEX idx_student_profiles_class_id (class_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_student_profiles_class FOREIGN KEY (class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生档案扩展表';

-- ----------------------------------------------------------------------------
-- 3.5 教师档案扩展表
-- ----------------------------------------------------------------------------
CREATE TABLE teacher_profiles (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    college_id  BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '所属学院',
    title       VARCHAR(50)     NULL DEFAULT NULL COMMENT '职称',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_profiles_user (user_id),
    INDEX idx_teacher_profiles_college_id (college_id),
    CONSTRAINT fk_teacher_profiles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_teacher_profiles_college FOREIGN KEY (college_id) REFERENCES colleges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师档案扩展表';

-- ----------------------------------------------------------------------------
-- 3.6 用户兴趣表
-- ----------------------------------------------------------------------------
CREATE TABLE user_interests (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id            BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    tag_name           VARCHAR(100)    NOT NULL COMMENT '兴趣标签名（如"编程"、"阅读"）',
    proficiency_level  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '熟练度：1入门 2一般 3熟练 4精通',
    weight             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '权重/出现次数（画像算法用）',
    sort               INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    is_detail          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=系统标签（自动提取）1=用户手动添加的详细兴趣',
    detail_content     VARCHAR(255)    NULL DEFAULT NULL COMMENT '具体内容描述（is_detail=1 时填写）',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_interests_user_id (user_id),
    UNIQUE KEY uk_user_interests_tag (user_id, tag_name, is_detail),
    CONSTRAINT fk_user_interests_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣表';

-- ----------------------------------------------------------------------------
-- 3.7 个人奖项汇总表
-- ----------------------------------------------------------------------------
CREATE TABLE award_summaries (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    category     VARCHAR(50)     NOT NULL COMMENT '类别',
    total_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总次数',
    max_level    VARCHAR(50)     NULL DEFAULT NULL COMMENT '最高级别',
    latest_time  DATE            NULL DEFAULT NULL COMMENT '最近一次时间',
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_summaries_user_cat (user_id, category),
    INDEX idx_award_summaries_user_id (user_id),
    CONSTRAINT fk_award_summaries_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人奖项汇总表';

-- ----------------------------------------------------------------------------
-- 3.8 用户消息通知设置表
-- ----------------------------------------------------------------------------
CREATE TABLE notification_settings (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    category       VARCHAR(50)     NOT NULL COMMENT '通知分类：audit_remind/system_notice/dynamic_remind',
    email_enabled  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=关闭 1=开启邮件通知',
    sms_enabled    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=关闭 1=开启短信通知',
    push_enabled   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=关闭 1=开启站内推送',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_settings (user_id, category),
    CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息通知设置表';

-- ----------------------------------------------------------------------------
-- 3.9 用户收藏/快捷入口表
-- ----------------------------------------------------------------------------
CREATE TABLE user_favorites (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    favorite_type   VARCHAR(50)     NOT NULL COMMENT '收藏类型',
    target_id       VARCHAR(100)    NOT NULL COMMENT '目标标识',
    target_name     VARCHAR(100)    NOT NULL COMMENT '目标名称',
    icon            VARCHAR(100)    NULL DEFAULT NULL COMMENT '图标类名',
    sort            INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    last_used_at    TIMESTAMP       NULL DEFAULT NULL COMMENT '最近使用时间',
    use_count       INT UNSIGNED    NULL DEFAULT 0 COMMENT '使用次数',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_favorites_user_id (user_id),
    UNIQUE KEY uk_user_favorites (user_id, favorite_type, target_id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏/快捷入口表';
