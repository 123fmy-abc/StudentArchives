-- ============================================================================
-- V1: 基础表（Foundation Tables）
-- 描述：学校、学院、专业、班级、学期、能力维度、数据字典、系统配置、表单模板、消息模板
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 学校表
-- ----------------------------------------------------------------------------
CREATE TABLE schools (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schools_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校表';

-- ----------------------------------------------------------------------------
-- 1.2 学期表
-- ----------------------------------------------------------------------------
CREATE TABLE semesters (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)     NOT NULL,
    start_date  DATE            NOT NULL,
    end_date    DATE            NOT NULL,
    is_current  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否当前学期',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_semesters_name (name),
    INDEX idx_semesters_is_current (is_current),
    CONSTRAINT chk_semesters_dates CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';

-- ----------------------------------------------------------------------------
-- 1.3 能力维度字典表
-- ----------------------------------------------------------------------------
CREATE TABLE ability_dimensions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dimension_name  VARCHAR(50)     NOT NULL,
    dimension_code  VARCHAR(50)     NOT NULL,
    description     VARCHAR(255)    NULL DEFAULT NULL,
    sort            INT UNSIGNED    NOT NULL DEFAULT 0,
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ability_dimensions_code (dimension_code),
    INDEX idx_ability_dimensions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='能力维度字典表';

-- ----------------------------------------------------------------------------
-- 1.4 数据字典表
-- ----------------------------------------------------------------------------
CREATE TABLE dictionaries (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dict_type   VARCHAR(50)     NOT NULL COMMENT '字典类型编码',
    dict_code   VARCHAR(50)     NOT NULL COMMENT '字典项编码',
    dict_name   VARCHAR(100)    NOT NULL COMMENT '字典项名称',
    parent_id   BIGINT UNSIGNED NULL DEFAULT NULL,
    sort        INT UNSIGNED    NOT NULL DEFAULT 0,
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dictionaries_type_code (dict_type, dict_code),
    INDEX idx_dictionaries_dict_type (dict_type),
    INDEX idx_dictionaries_parent_id (parent_id),
    CONSTRAINT fk_dictionaries_parent FOREIGN KEY (parent_id) REFERENCES dictionaries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- ----------------------------------------------------------------------------
-- 1.5 系统配置表
-- ----------------------------------------------------------------------------
CREATE TABLE system_settings (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    setting_key     VARCHAR(100)    NOT NULL COMMENT '配置键',
    setting_value   TEXT            NOT NULL COMMENT '配置值',
    setting_group   VARCHAR(50)     NOT NULL COMMENT '配置分组',
    value_type      VARCHAR(20)     NOT NULL DEFAULT 'string' COMMENT '值类型：string/int/float/json/boolean',
    description     VARCHAR(255)    NULL DEFAULT NULL COMMENT '配置说明',
    is_editable     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=只读 1=可编辑',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_settings_key (setting_key),
    INDEX idx_system_settings_group (setting_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ----------------------------------------------------------------------------
-- 1.6 表单自定义模板表
-- ----------------------------------------------------------------------------
CREATE TABLE form_templates (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL COMMENT '模板名称',
    code        VARCHAR(50)     NOT NULL COMMENT '模板编码，对应 archives.archive_type',
    fields      JSON            NOT NULL COMMENT '字段配置JSON',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_templates_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单自定义模板表';

-- ----------------------------------------------------------------------------
-- 1.7 消息模板表
-- ----------------------------------------------------------------------------
CREATE TABLE message_templates (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_code     VARCHAR(50)     NOT NULL COMMENT '模板编码',
    template_name     VARCHAR(100)    NOT NULL COMMENT '模板名称',
    category          VARCHAR(50)     NOT NULL COMMENT '分类：audit_remind/system_notice/dynamic_remind',
    title_template    VARCHAR(255)    NOT NULL COMMENT '标题模板，支持变量如 {{title}}',
    content_template  TEXT            NOT NULL COMMENT '内容模板，支持变量',
    variables         JSON            NULL DEFAULT NULL COMMENT '变量定义：["title","applicant_name","submit_time"]',
    status            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_templates_code (template_code),
    INDEX idx_message_templates_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';
