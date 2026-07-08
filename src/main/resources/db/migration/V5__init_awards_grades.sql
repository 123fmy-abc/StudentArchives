-- ============================================================================
-- V5: 奖项报名与成绩管理表（Award Applications & Grades）
-- 描述：奖项报名基表/版本/类型配置、奖项扩展表、成绩绩点、学期汇总、画像评分
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 奖项类型配置表
-- ----------------------------------------------------------------------------
CREATE TABLE award_type_config (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    award_type     VARCHAR(50)     NOT NULL COMMENT '奖项类型编码',
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
    UNIQUE KEY uk_award_type_config_type (award_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项类型配置表';

-- ----------------------------------------------------------------------------
-- 5.2 奖项报名基表
-- ----------------------------------------------------------------------------
CREATE TABLE award_applications (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    award_type       VARCHAR(50)     NOT NULL COMMENT '奖项类型',
    title            VARCHAR(255)    NOT NULL COMMENT '报名标题',
    semester_id      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
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
    INDEX idx_award_apps_user_type (user_id, award_type),
    INDEX idx_award_apps_status (status),
    INDEX idx_award_apps_semester_id (semester_id),
    INDEX idx_award_apps_school_id (school_id),
    CONSTRAINT fk_award_apps_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_award_apps_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_award_apps_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_award_apps_auditor FOREIGN KEY (auditor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项报名基表';

-- ----------------------------------------------------------------------------
-- 5.3 奖项版本历史表
-- ----------------------------------------------------------------------------
CREATE TABLE award_versions (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id   BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    version          INT UNSIGNED    NOT NULL COMMENT '版本号',
    title            VARCHAR(255)    NOT NULL COMMENT '报名标题',
    data_snapshot    JSON            NOT NULL COMMENT '完整数据快照',
    status           TINYINT UNSIGNED NOT NULL COMMENT '该版本状态',
    rejected_reason  TEXT            NULL DEFAULT NULL COMMENT '驳回原因',
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_versions (application_id, version),
    INDEX idx_award_versions_app (application_id),
    CONSTRAINT fk_award_versions_app FOREIGN KEY (application_id) REFERENCES award_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项版本历史表';

-- ----------------------------------------------------------------------------
-- 5.4 奖项扩展表 — 竞赛之星
-- ----------------------------------------------------------------------------
CREATE TABLE app_competition_stars (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    competition_name   VARCHAR(255)    NOT NULL COMMENT '竞赛名称',
    participate_time   DATE            NULL DEFAULT NULL COMMENT '参赛时间',
    competition_level  VARCHAR(50)     NULL DEFAULT NULL COMMENT '竞赛级别',
    award_level        VARCHAR(50)     NULL DEFAULT NULL COMMENT '获奖级别',
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_competition_stars (application_id),
    CONSTRAINT fk_app_competition_stars_app FOREIGN KEY (application_id) REFERENCES award_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛之星扩展表';

-- ----------------------------------------------------------------------------
-- 5.5 奖项扩展表 — 科研之星
-- ----------------------------------------------------------------------------
CREATE TABLE app_research_stars (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_research_stars (application_id),
    CONSTRAINT fk_app_research_stars_app FOREIGN KEY (application_id) REFERENCES award_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研之星扩展表';

-- 5.5a 科研项目子表
CREATE TABLE app_research_projects (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    research_star_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 app_research_stars.id',
    project_name      VARCHAR(255)    NOT NULL COMMENT '项目名称',
    project_level     VARCHAR(50)     NULL DEFAULT NULL COMMENT '项目级别',
    rank_total        VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    establish_time    DATE            NULL DEFAULT NULL COMMENT '立项时间',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_app_research_projects_star (research_star_id),
    CONSTRAINT fk_app_research_projects_star FOREIGN KEY (research_star_id) REFERENCES app_research_stars(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研项目子表';

-- 5.5b 软件著作权子表
CREATE TABLE app_software_copyrights (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    research_star_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 app_research_stars.id',
    software_name     VARCHAR(255)    NOT NULL COMMENT '软著名称',
    issuing_unit      VARCHAR(255)    NULL DEFAULT NULL COMMENT '颁发单位',
    rank_total        VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    approval_time     DATE            NULL DEFAULT NULL COMMENT '获批时间',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_app_software_copyrights_star (research_star_id),
    CONSTRAINT fk_app_software_copyrights_star FOREIGN KEY (research_star_id) REFERENCES app_research_stars(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件著作权子表';

-- 5.5c 发表论文子表
CREATE TABLE app_published_papers (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    research_star_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 app_research_stars.id',
    journal_name      VARCHAR(255)    NOT NULL COMMENT '期刊名称',
    paper_title       VARCHAR(255)    NOT NULL COMMENT '论文名称',
    rank_total        VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    publish_time      DATE            NULL DEFAULT NULL COMMENT '发表时间',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_app_published_papers_star (research_star_id),
    CONSTRAINT fk_app_published_papers_star FOREIGN KEY (research_star_id) REFERENCES app_research_stars(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发表论文子表';

-- ----------------------------------------------------------------------------
-- 5.6 奖项扩展表 — 双创之星
-- ----------------------------------------------------------------------------
CREATE TABLE app_innovation_stars (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    company_name    VARCHAR(255)    NOT NULL COMMENT '公司名称',
    industry_type   VARCHAR(100)    NULL DEFAULT NULL COMMENT '行业类型',
    applicant_rank  VARCHAR(50)     NULL DEFAULT NULL COMMENT '申报人排名',
    register_time   DATE            NULL DEFAULT NULL COMMENT '注册时间',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_innovation_stars (application_id),
    CONSTRAINT fk_app_innovation_stars_app FOREIGN KEY (application_id) REFERENCES award_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双创之星扩展表';

-- ----------------------------------------------------------------------------
-- 5.7 成绩/绩点表
-- ----------------------------------------------------------------------------
CREATE TABLE gpa_records (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id    BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    semester_id  BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    course_code  VARCHAR(50)     NULL DEFAULT NULL COMMENT '课程编码（学校课程编号）',
    course_name  VARCHAR(255)    NOT NULL COMMENT '课程名称',
    attempt_no   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '修读次数：1=首次 2=重修...',
    score        DECIMAL(5,2)    NULL DEFAULT NULL COMMENT '期末成绩',
    gpa          DECIMAL(3,2)    NULL DEFAULT NULL COMMENT '绩点（导入时计算好存入）',
    credit       DECIMAL(3,1)    NULL DEFAULT NULL COMMENT '学分',
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_gpa_records_user_semester (user_id, semester_id),
    UNIQUE KEY uk_gpa_records (user_id, semester_id, course_code, course_name, attempt_no),
    INDEX idx_gpa_records_school_id (school_id),
    CONSTRAINT chk_gpa_records_score CHECK (score >= 0 AND score <= 100),
    CONSTRAINT chk_gpa_records_gpa CHECK (gpa >= 0 AND gpa <= 5.00),
    CONSTRAINT fk_gpa_records_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_gpa_records_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_gpa_records_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩/绩点表';

-- ----------------------------------------------------------------------------
-- 5.8 学期成绩汇总表
-- ----------------------------------------------------------------------------
CREATE TABLE semester_gpa_summaries (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    semester_id    BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    course_count   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '课程数',
    total_credit   DECIMAL(6,2)    NOT NULL DEFAULT 0 COMMENT '总学分',
    weighted_gpa   DECIMAL(3,2)    NOT NULL DEFAULT 0 COMMENT '加权平均绩点',
    average_score  DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '平均分',
    rank_in_class  INT UNSIGNED    NULL DEFAULT NULL COMMENT '班级排名',
    rank_in_major  INT UNSIGNED    NULL DEFAULT NULL COMMENT '专业排名',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_semester_gpa_summaries (user_id, semester_id),
    INDEX idx_semester_gpa_summaries_user (user_id),
    CONSTRAINT fk_semester_gpa_summaries_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_semester_gpa_summaries_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期成绩汇总表';

-- ----------------------------------------------------------------------------
-- 5.9 画像评估分数表
-- ----------------------------------------------------------------------------
CREATE TABLE portrait_evaluation_scores (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    semester_id     BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    dimension_code  VARCHAR(50)     NOT NULL COMMENT '维度编码，关联 ability_dimensions.dimension_code',
    score           DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '当前得分',
    target_score    DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '目标分',
    `change`        DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '较上阶段变化',
    gap             DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '距目标分数',
    evaluated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portrait_eval (user_id, semester_id, dimension_code),
    INDEX idx_portrait_eval_user (user_id),
    CONSTRAINT fk_portrait_eval_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_portrait_eval_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像评估分数表';

-- ----------------------------------------------------------------------------
-- 5.10 成绩导入历史表
-- ----------------------------------------------------------------------------
CREATE TABLE grade_import_logs (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_id    BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    semester_id    BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    file_id        BIGINT UNSIGNED NOT NULL COMMENT '关联文件',
    total_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    success_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    fail_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    fail_details   JSON            NULL DEFAULT NULL COMMENT '失败详情',
    import_status  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=导入中 1=完成 2=失败',
    started_at     TIMESTAMP       NULL DEFAULT NULL COMMENT '开始时间',
    completed_at   TIMESTAMP       NULL DEFAULT NULL COMMENT '完成时间',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_grade_import_operator FOREIGN KEY (operator_id) REFERENCES users(id),
    CONSTRAINT fk_grade_import_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT fk_grade_import_file FOREIGN KEY (file_id) REFERENCES file_uploads(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩导入历史表';
