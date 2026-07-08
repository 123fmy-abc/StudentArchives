-- ============================================================================
-- V4: 档案管理表（Archive Management）
-- 描述：档案基表、版本历史、类型配置、10张档案扩展表
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 档案类型配置表
-- ----------------------------------------------------------------------------
CREATE TABLE archive_type_config (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_type   VARCHAR(50)     NOT NULL COMMENT '档案类型英文编码',
    type_name      VARCHAR(50)     NOT NULL COMMENT '中文名称',
    evaluate_desc  TEXT            NULL DEFAULT NULL COMMENT '评选说明',
    apply_desc     TEXT            NULL DEFAULT NULL COMMENT '申报说明',
    icon           VARCHAR(100)    NULL DEFAULT NULL COMMENT '图标类名',
    sort           INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_type_config_type (archive_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案类型配置表';

-- ----------------------------------------------------------------------------
-- 4.2 档案基表
-- ----------------------------------------------------------------------------
CREATE TABLE archives (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    archive_type     VARCHAR(50)     NOT NULL COMMENT '档案类型（英文编码）',
    title            VARCHAR(255)    NOT NULL COMMENT '档案标题',
    semester_id      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    obtain_time      DATE            NULL DEFAULT NULL COMMENT '获得/发生时间',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审批 2=通过 3=驳回 4=需修改',
    rejected_reason  TEXT            NULL DEFAULT NULL COMMENT '驳回原因',
    submit_time      DATETIME        NULL DEFAULT NULL COMMENT '提交时间',
    audited_at       DATETIME        NULL DEFAULT NULL COMMENT '审核时间',
    auditor_id       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
    current_version  INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    submit_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '提交次数',
    draft_saved_at   TIMESTAMP       NULL DEFAULT NULL COMMENT '草稿自动保存时间',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_archives_user_type (user_id, archive_type),
    INDEX idx_archives_status (status),
    INDEX idx_archives_semester_id (semester_id),
    INDEX idx_archives_user_obtain (user_id, obtain_time),
    INDEX idx_archives_submit_time (submit_time),
    INDEX idx_archives_school_id (school_id),
    CONSTRAINT fk_archives_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_archives_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_archives_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_archives_auditor FOREIGN KEY (auditor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案基表';

-- ----------------------------------------------------------------------------
-- 4.3 档案版本历史表
-- ----------------------------------------------------------------------------
CREATE TABLE archive_versions (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    version          INT UNSIGNED    NOT NULL COMMENT '版本号',
    title            VARCHAR(255)    NOT NULL COMMENT '档案标题',
    data_snapshot    JSON            NOT NULL COMMENT '完整数据快照',
    status           TINYINT UNSIGNED NOT NULL COMMENT '该版本状态',
    rejected_reason  TEXT            NULL DEFAULT NULL COMMENT '驳回原因',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_versions (archive_id, version),
    CONSTRAINT fk_archive_versions_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案版本历史表';

-- ----------------------------------------------------------------------------
-- 4.4 档案扩展表 — 学科竞赛
-- ----------------------------------------------------------------------------
CREATE TABLE archive_competitions (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    competition_name   VARCHAR(255)    NOT NULL COMMENT '竞赛名称',
    competition_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '竞赛类型',
    award_level        VARCHAR(50)     NULL DEFAULT NULL COMMENT '获奖等级',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_competitions (archive_id),
    CONSTRAINT fk_archive_competitions_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科竞赛扩展表';

-- ----------------------------------------------------------------------------
-- 4.5 档案扩展表 — 创新创业
-- ----------------------------------------------------------------------------
CREATE TABLE archive_innovations (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    company_name   VARCHAR(255)    NOT NULL COMMENT '公司名称',
    industry_type  VARCHAR(100)    NULL DEFAULT NULL COMMENT '行业类型',
    project_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '公司类型',
    team_role      VARCHAR(50)     NULL DEFAULT NULL COMMENT '团队角色',
    register_time  DATE            NULL DEFAULT NULL COMMENT '注册时间',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_innovations (archive_id),
    CONSTRAINT fk_archive_innovations_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创新创业扩展表';

-- ----------------------------------------------------------------------------
-- 4.6 档案扩展表 — 学术研究
-- ----------------------------------------------------------------------------
CREATE TABLE archive_researches (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    project_name   VARCHAR(255)    NOT NULL COMMENT '项目名称',
    project_level  VARCHAR(50)     NULL DEFAULT NULL COMMENT '项目级别',
    project_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '项目类型',
    team_role      VARCHAR(50)     NULL DEFAULT NULL COMMENT '团队角色',
    project_time   VARCHAR(20)     NULL DEFAULT NULL COMMENT '项目时间，格式如 2024-06',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_researches (archive_id),
    CONSTRAINT fk_archive_researches_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学术研究扩展表';

-- ----------------------------------------------------------------------------
-- 4.7 档案扩展表 — 奖学金
-- ----------------------------------------------------------------------------
CREATE TABLE archive_scholarships (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id            BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    scholarship_name      VARCHAR(255)    NOT NULL COMMENT '奖学金名称',
    scholarship_category  VARCHAR(50)     NULL DEFAULT NULL COMMENT '奖学金类别：国家/省/校/企业',
    award_level           VARCHAR(50)     NULL DEFAULT NULL COMMENT '获奖等级：一/二/三等奖',
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_scholarships (archive_id),
    CONSTRAINT fk_archive_scholarships_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖学金扩展表';

-- ----------------------------------------------------------------------------
-- 4.8 档案扩展表 — 荣誉证书
-- ----------------------------------------------------------------------------
CREATE TABLE archive_certificates (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    certificate_type  VARCHAR(100)    NULL DEFAULT NULL COMMENT '证书类型',
    certificate_name  VARCHAR(255)    NOT NULL COMMENT '证书名称',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_certificates (archive_id),
    CONSTRAINT fk_archive_certificates_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉证书扩展表';

-- ----------------------------------------------------------------------------
-- 4.9 档案扩展表 — 实习经历
-- ----------------------------------------------------------------------------
CREATE TABLE archive_internships (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id    BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    company_name  VARCHAR(255)    NOT NULL COMMENT '实习单位',
    location      VARCHAR(255)    NULL DEFAULT NULL COMMENT '实习地点',
    position      VARCHAR(100)    NULL DEFAULT NULL COMMENT '实习岗位',
    start_date    DATE            NULL DEFAULT NULL COMMENT '开始日期',
    end_date      DATE            NULL DEFAULT NULL COMMENT '结束日期',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_internships (archive_id),
    CONSTRAINT fk_archive_internships_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实习经历扩展表';

-- ----------------------------------------------------------------------------
-- 4.10 档案扩展表 — 组织履历
-- ----------------------------------------------------------------------------
CREATE TABLE archive_organizations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    org_level       VARCHAR(50)     NULL DEFAULT NULL COMMENT '组织级别',
    department      VARCHAR(100)    NULL DEFAULT NULL COMMENT '所在部门',
    position_title  VARCHAR(100)    NULL DEFAULT NULL COMMENT '职位',
    start_date      DATE            NULL DEFAULT NULL COMMENT '任职开始',
    end_date        DATE            NULL DEFAULT NULL COMMENT '任职结束',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_organizations (archive_id),
    CONSTRAINT fk_archive_organizations_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织履历扩展表';

-- ----------------------------------------------------------------------------
-- 4.11 档案扩展表 — 实训项目
-- ----------------------------------------------------------------------------
CREATE TABLE archive_training_projects (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    project_name     VARCHAR(255)    NOT NULL COMMENT '项目名称',
    project_content  TEXT            NULL DEFAULT NULL COMMENT '项目内容',
    start_date       DATE            NULL DEFAULT NULL COMMENT '开始日期',
    end_date         DATE            NULL DEFAULT NULL COMMENT '结束日期',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_training_projects (archive_id),
    CONSTRAINT fk_archive_training_projects_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训项目扩展表';

-- ----------------------------------------------------------------------------
-- 4.12 档案扩展表 — 社会实践
-- ----------------------------------------------------------------------------
CREATE TABLE archive_social_practices (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    activity_name      VARCHAR(255)    NOT NULL COMMENT '活动名称',
    practice_location  VARCHAR(255)    NULL DEFAULT NULL COMMENT '活动地点',
    practice_unit      VARCHAR(255)    NULL DEFAULT NULL COMMENT '组织单位',
    start_date         DATE            NULL DEFAULT NULL COMMENT '开始日期',
    end_date           DATE            NULL DEFAULT NULL COMMENT '结束日期',
    volunteer_hours    DECIMAL(8,2)    NULL DEFAULT 0.00 COMMENT '志愿时长',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_social_practices (archive_id),
    CONSTRAINT fk_archive_social_practices_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社会实践扩展表';

-- ----------------------------------------------------------------------------
-- 4.13 档案扩展表 — 图书心得
-- ----------------------------------------------------------------------------
CREATE TABLE archive_book_reviews (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    archive_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    book_name       VARCHAR(255)    NOT NULL COMMENT '书名',
    read_month      VARCHAR(20)     NULL DEFAULT NULL COMMENT '阅读时间，格式如 2024-06',
    review_content  TEXT            NOT NULL COMMENT '心得体会',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_book_reviews (archive_id),
    CONSTRAINT fk_archive_book_reviews_archive FOREIGN KEY (archive_id) REFERENCES archives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书心得扩展表';
