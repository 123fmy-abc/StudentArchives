-- ============================================================
-- V6: Award Application Tables
-- 奖项报名基表、类型配置、竞赛之星、科研之星（含子表）、双创之星
-- ============================================================

-- 1. award_type_configs — 奖项类型配置表
CREATE TABLE `award_type_configs` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `award_type`       VARCHAR(50)     NOT NULL COMMENT '奖项类型编码',
    `type_name`        VARCHAR(50)     NOT NULL COMMENT '中文名称',
    `evaluate_desc`    TEXT            NULL DEFAULT NULL COMMENT '评选说明',
    `apply_desc`       TEXT            NULL DEFAULT NULL COMMENT '申报说明',
    `icon`             VARCHAR(100)    NULL DEFAULT NULL COMMENT '图标类名',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `updated_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '更新人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_atc_award_type` (`award_type`, `is_deleted_null`),
    CONSTRAINT `fk_awtc_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_awtc_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项类型配置表';


-- 2. award_applications — 奖项报名基表
CREATE TABLE `award_applications` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `award_type`       VARCHAR(50)     NOT NULL COMMENT '奖项类型',
    `title`            VARCHAR(255)    NOT NULL COMMENT '报名标题',
    `semester_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `certificate_no`   VARCHAR(100)    NULL DEFAULT NULL COMMENT '证书编号',
    `issuing_unit`     VARCHAR(255)    NULL DEFAULT NULL COMMENT '发证/主办单位',
    `valid_until`      DATE            NULL DEFAULT NULL COMMENT '证书有效期',
    `participant_role` VARCHAR(50)     NULL DEFAULT NULL COMMENT '本人角色',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审批 2=通过 3=已退回 4=已撤销',
    `rejected_reason`  TEXT            NULL DEFAULT NULL COMMENT '退回原因',
    `submitted_at`     DATETIME        NULL DEFAULT NULL COMMENT '提交时间',
    `audited_at`       DATETIME        NULL DEFAULT NULL COMMENT '审核时间',
    `auditor_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
    `returned_at`      DATETIME        NULL DEFAULT NULL COMMENT '退回时间',
    `passed_at`        DATETIME        NULL DEFAULT NULL COMMENT '通过时间',
    `revoked_at`       DATETIME        NULL DEFAULT NULL COMMENT '撤销时间',
    `current_version`  INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `submit_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '提交次数',
    `draft_saved_at`   DATETIME        NULL DEFAULT NULL COMMENT '草稿自动保存时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_aa_user_type` (`user_id`, `award_type`),
    INDEX `idx_aa_status` (`status`),
    INDEX `idx_aa_semester_id` (`semester_id`),
    INDEX `idx_aa_school_id` (`school_id`),
    INDEX `idx_aa_user_status` (`user_id`, `status`),
    INDEX `idx_aa_school_status_time` (`school_id`, `status`, `submitted_at`),
    INDEX `idx_aa_user_status_time` (`user_id`, `status`, `submitted_at`),
    CONSTRAINT `ck_aa_rejected` CHECK (`status` != 3 OR `rejected_reason` IS NOT NULL),
    CONSTRAINT `fk_aa_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_aa_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_aa_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_aa_auditor_id` FOREIGN KEY (`auditor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖项报名基表';


-- 3. award_competition_stars — 竞赛之星
CREATE TABLE `award_competition_stars` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `application_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    `competition_name`  VARCHAR(255)    NOT NULL COMMENT '竞赛名称',
    `participated_at`   DATE            NULL DEFAULT NULL COMMENT '参赛时间',
    `competition_level` VARCHAR(50)     NOT NULL COMMENT '竞赛级别',
    `award_level`       VARCHAR(50)     NOT NULL COMMENT '获奖级别',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_acs_application_id` (`application_id`),
    CONSTRAINT `fk_acs_application_id` FOREIGN KEY (`application_id`) REFERENCES `award_applications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛之星';


-- 4. award_research_stars — 科研之星
CREATE TABLE `award_research_stars` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `application_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    `primary_category` VARCHAR(50)     NOT NULL COMMENT '主要成果类型',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ars_application_id` (`application_id`),
    CONSTRAINT `ck_ars_primary_category` CHECK (`primary_category` IN ('project', 'software_copyright', 'published_paper')),
    CONSTRAINT `fk_ars_application_id` FOREIGN KEY (`application_id`) REFERENCES `award_applications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研之星';


-- 5. award_research_projects — 科研项目子表
CREATE TABLE `award_research_projects` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `research_star_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 award_research_stars.id',
    `project_name`     VARCHAR(255)    NOT NULL COMMENT '项目名称',
    `project_level`    VARCHAR(50)     NULL DEFAULT NULL COMMENT '项目级别',
    `rank_total`       VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    `established_at`   DATE            NULL DEFAULT NULL COMMENT '立项时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_arp_research_star_id` (`research_star_id`),
    CONSTRAINT `fk_arp_research_star_id` FOREIGN KEY (`research_star_id`) REFERENCES `award_research_stars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研项目子表';


-- 6. award_software_copyrights — 软件著作权子表
CREATE TABLE `award_software_copyrights` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `research_star_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 award_research_stars.id',
    `software_name`    VARCHAR(255)    NOT NULL COMMENT '软著名称',
    `rank_total`       VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    `approved_at`      DATE            NULL DEFAULT NULL COMMENT '获批时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_asc_research_star_id` (`research_star_id`),
    CONSTRAINT `fk_asc_research_star_id` FOREIGN KEY (`research_star_id`) REFERENCES `award_research_stars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件著作权子表';


-- 7. award_published_papers — 发表论文子表
CREATE TABLE `award_published_papers` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `research_star_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 award_research_stars.id',
    `journal_name`     VARCHAR(255)    NOT NULL COMMENT '期刊名称',
    `paper_title`      VARCHAR(255)    NOT NULL COMMENT '论文名称',
    `rank_total`       VARCHAR(50)     NULL DEFAULT NULL COMMENT '排名/总人数',
    `published_at`     DATE            NULL DEFAULT NULL COMMENT '发表时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_app_research_star_id` (`research_star_id`),
    CONSTRAINT `fk_app_research_star_id` FOREIGN KEY (`research_star_id`) REFERENCES `award_research_stars` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发表论文子表';


-- 8. award_innovation_stars — 双创之星
CREATE TABLE `award_innovation_stars` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `application_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联 award_applications.id',
    `company_name`     VARCHAR(255)    NOT NULL COMMENT '公司名称',
    `industry_type`    VARCHAR(100)    NOT NULL COMMENT '行业类型',
    `applicant_rank`   VARCHAR(50)     NULL DEFAULT NULL COMMENT '申报人排名',
    `registered_at`    DATE            NULL DEFAULT NULL COMMENT '注册时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ais_application_id` (`application_id`),
    CONSTRAINT `fk_ais_application_id` FOREIGN KEY (`application_id`) REFERENCES `award_applications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双创之星';
