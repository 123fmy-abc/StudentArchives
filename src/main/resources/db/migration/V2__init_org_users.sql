-- ============================================================================
-- V2: 组织架构与用户权限表（Organization, Users & Permissions）
-- 描述：学院、专业、班级、用户、角色、权限、文件上传
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 学院表
-- ----------------------------------------------------------------------------
CREATE TABLE colleges (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id   BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    name        VARCHAR(100)    NOT NULL COMMENT '学院名称',
    code        VARCHAR(50)     NOT NULL COMMENT '学院编码',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_colleges_school_id (school_id),
    UNIQUE KEY uk_colleges_code (code),
    CONSTRAINT fk_colleges_school FOREIGN KEY (school_id) REFERENCES schools(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';

-- ----------------------------------------------------------------------------
-- 2.2 专业表
-- ----------------------------------------------------------------------------
CREATE TABLE majors (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    college_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 colleges.id',
    name        VARCHAR(100)    NOT NULL COMMENT '专业名称',
    code        VARCHAR(50)     NOT NULL COMMENT '专业编码',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_majors_college_id (college_id),
    UNIQUE KEY uk_majors_code (code),
    CONSTRAINT fk_majors_college FOREIGN KEY (college_id) REFERENCES colleges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- ----------------------------------------------------------------------------
-- 2.3 班级表
-- ----------------------------------------------------------------------------
CREATE TABLE classes (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    major_id       BIGINT UNSIGNED NOT NULL COMMENT '关联 majors.id',
    name           VARCHAR(100)    NOT NULL COMMENT '班级名称',
    grade          VARCHAR(20)     NOT NULL COMMENT '年级，如 2023级',
    student_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '学生人数',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_classes_major_id (major_id),
    INDEX idx_classes_grade (grade),
    CONSTRAINT fk_classes_major FOREIGN KEY (major_id) REFERENCES majors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ----------------------------------------------------------------------------
-- 2.4 用户表
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id，多校数据隔离',
    user_no           VARCHAR(50)     NOT NULL COMMENT '统一编号：学号/工号/管理员工号',
    name              VARCHAR(100)    NOT NULL COMMENT '姓名',
    gender            TINYINT UNSIGNED NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    email             VARCHAR(255)    NULL DEFAULT NULL COMMENT '邮箱',
    phone             VARCHAR(20)     NULL DEFAULT NULL COMMENT '电话',
    password          VARCHAR(255)    NOT NULL COMMENT 'Bcrypt哈希',
    avatar            VARCHAR(255)    NULL DEFAULT NULL COMMENT '头像',
    remember_token    VARCHAR(100)    NULL DEFAULT NULL COMMENT '记住我令牌',
    email_verified_at TIMESTAMP       NULL DEFAULT NULL COMMENT '邮箱验证时间',
    status            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_user_no (user_no),
    UNIQUE KEY uk_users_school_email (school_id, email),
    INDEX idx_users_status (status),
    INDEX idx_users_school_id (school_id),
    CONSTRAINT fk_users_school FOREIGN KEY (school_id) REFERENCES schools(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- 2.5 角色表
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name             VARCHAR(50)     NOT NULL COMMENT '角色名称（学校自定义）',
    code             VARCHAR(50)     NOT NULL COMMENT '角色编码（系统生成，唯一）',
    description      VARCHAR(255)    NULL DEFAULT NULL COMMENT '角色描述',
    level            TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色层级：0=系统 1=学生 2=教师 3=辅导员 4=系主任 5=院长 6=校长 7+=自定义',
    is_system        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=学校自定义 1=系统内置（不可删除）',
    is_auditor       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=非审批角色 1=可作为审批节点',
    scope_types      JSON             NULL DEFAULT NULL COMMENT '允许绑定的范围类型：[2,3,4] 表示可绑学院/专业/班级',
    max_scope_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '最大绑定数量：0=无限制',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code),
    INDEX idx_roles_level (level),
    INDEX idx_roles_system_status (is_system, status),
    INDEX idx_roles_auditor_status (is_auditor, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------------------------------------------------------
-- 2.6 权限表
-- ----------------------------------------------------------------------------
CREATE TABLE permissions (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL COMMENT '权限名称',
    code        VARCHAR(100)    NOT NULL COMMENT '权限编码',
    type        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=menu菜单权限 2=button按钮权限 3=api接口权限',
    parent_id   BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '父权限ID',
    sort        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code),
    INDEX idx_permissions_parent_id (parent_id),
    CONSTRAINT fk_permissions_parent FOREIGN KEY (parent_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------------------------------------------------------
-- 2.7 文件上传管理表
-- ----------------------------------------------------------------------------
CREATE TABLE file_uploads (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
    fileable_type   VARCHAR(50)     NOT NULL COMMENT '关联模型短编码：archive/award/career_plan/announcement等',
    fileable_id     BIGINT UNSIGNED NOT NULL COMMENT '关联模型ID',
    file_category   VARCHAR(50)     NULL DEFAULT NULL COMMENT '附件分类：certificate/photo/proof/other',
    original_name   VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    file_path       VARCHAR(500)    NOT NULL COMMENT '存储路径',
    file_size       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '文件大小（字节）',
    mime_type       VARCHAR(100)    NULL DEFAULT NULL COMMENT 'MIME类型',
    disk            VARCHAR(50)     NOT NULL DEFAULT 'local' COMMENT '存储磁盘',
    sort_order      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_file_uploads_fileable (fileable_type, fileable_id),
    INDEX idx_file_uploads_user_id (user_id),
    CONSTRAINT fk_file_uploads_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传管理表';
