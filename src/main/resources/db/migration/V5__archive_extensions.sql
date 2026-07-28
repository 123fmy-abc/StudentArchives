-- ============================================================
-- V5: Archive Extension Tables (10 tables)
-- 档案扩展表：竞赛、创新、研究、奖学金、证书、实习、组织、实训、实践、图书心得
-- ============================================================

-- 1. archive_competitions — 学科竞赛
CREATE TABLE `archive_competitions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `competition_name` VARCHAR(255)    NOT NULL COMMENT '竞赛名称',
    `competition_type` VARCHAR(100)    NOT NULL COMMENT '竞赛类型',
    `award_level`      VARCHAR(50)     NOT NULL COMMENT '获奖等级',
    `participant_role` VARCHAR(50)     NULL DEFAULT NULL COMMENT '本人角色：负责人/成员/独立完成',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ac_archive_id` (`archive_id`),
    CONSTRAINT `fk_ac_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科竞赛';


-- 2. archive_innovations — 创新创业
CREATE TABLE `archive_innovations` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `company_name`     VARCHAR(255)    NOT NULL COMMENT '公司名称',
    `industry_type`    VARCHAR(100)    NOT NULL COMMENT '行业类型',
    `project_type`     VARCHAR(100)    NOT NULL COMMENT '公司类型',
    `participant_role` VARCHAR(50)     NULL DEFAULT NULL COMMENT '本人角色：创始人/合伙人/核心成员',
    `registered_at`    DATE            NULL DEFAULT NULL COMMENT '注册时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_archive_id` (`archive_id`),
    CONSTRAINT `fk_ai_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创新创业';


-- 3. archive_researches — 学术研究
CREATE TABLE `archive_researches` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `project_name`     VARCHAR(255)    NOT NULL COMMENT '项目名称',
    `project_level`    VARCHAR(50)     NOT NULL COMMENT '项目级别',
    `project_type`     VARCHAR(100)    NOT NULL COMMENT '项目类型/研究类型',
    `participant_role` VARCHAR(50)     NULL DEFAULT NULL COMMENT '本人角色：负责人/成员/第一作者/独立完成',
    `start_date`       DATE            NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`         DATE            NULL DEFAULT NULL COMMENT '结束日期',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ar_archive_id` (`archive_id`),
    CONSTRAINT `ck_ar_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_ar_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学术研究';


-- 4. archive_scholarships — 奖学金
CREATE TABLE `archive_scholarships` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`            BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `scholarship_name`      VARCHAR(255)    NOT NULL COMMENT '奖学金名称',
    `scholarship_category`  VARCHAR(50)     NOT NULL COMMENT '奖学金类别',
    `award_level`           VARCHAR(50)     NOT NULL COMMENT '获奖等级',
    `created_at`            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`            TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_as_archive_id` (`archive_id`),
    CONSTRAINT `fk_as_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='奖学金';


-- 5. archive_certificates — 荣誉证书
CREATE TABLE `archive_certificates` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `certificate_type` VARCHAR(100)    NOT NULL COMMENT '证书类型',
    `certificate_name` VARCHAR(255)    NOT NULL COMMENT '证书名称',
    `certificate_no`   VARCHAR(100)    NULL DEFAULT NULL COMMENT '证书编号',
    `issuing_unit`     VARCHAR(255)    NULL DEFAULT NULL COMMENT '发证/主办单位',
    `valid_until`      DATE            NULL DEFAULT NULL COMMENT '证书有效期',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ace_archive_id` (`archive_id`),
    CONSTRAINT `fk_ace_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉证书';


-- 6. archive_internships — 实习经历
CREATE TABLE `archive_internships` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `company_name` VARCHAR(255)    NOT NULL COMMENT '实习公司',
    `location`     VARCHAR(255)    NULL DEFAULT NULL COMMENT '实习地点',
    `position`     VARCHAR(100)    NULL DEFAULT NULL COMMENT '实习岗位',
    `start_date`   DATE            NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`     DATE            NULL DEFAULT NULL COMMENT '结束日期',
    `created_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`   TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_aint_archive_id` (`archive_id`),
    CONSTRAINT `ck_aint_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_aint_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实习经历';


-- 7. archive_organizations — 组织履历
CREATE TABLE `archive_organizations` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `org_level`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '组织级别',
    `department`     VARCHAR(100)    NULL DEFAULT NULL COMMENT '所在部门',
    `position_title` VARCHAR(100)    NULL DEFAULT NULL COMMENT '职位',
    `start_date`     DATE            NULL DEFAULT NULL COMMENT '任职开始',
    `end_date`       DATE            NULL DEFAULT NULL COMMENT '任职结束',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ao_archive_id` (`archive_id`),
    CONSTRAINT `ck_ao_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_ao_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织履历';


-- 8. archive_training_projects — 实训项目
CREATE TABLE `archive_training_projects` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `project_name`    VARCHAR(255)    NOT NULL COMMENT '项目名称',
    `project_content` TEXT            NULL DEFAULT NULL COMMENT '项目内容',
    `start_date`      DATE            NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`        DATE            NULL DEFAULT NULL COMMENT '结束日期',
    `created_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_atp_archive_id` (`archive_id`),
    CONSTRAINT `ck_atp_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_atp_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实训项目';


-- 9. archive_social_practices — 社会实践
CREATE TABLE `archive_social_practices` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `activity_name`     VARCHAR(255)    NOT NULL COMMENT '活动名称',
    `practice_location` VARCHAR(255)    NULL DEFAULT NULL COMMENT '活动地点',
    `practice_unit`     VARCHAR(255)    NULL DEFAULT NULL COMMENT '组织单位',
    `participant_role`  VARCHAR(50)     NULL DEFAULT NULL COMMENT '本人角色：负责人/成员',
    `start_date`        DATE            NULL DEFAULT NULL COMMENT '开始日期',
    `end_date`          DATE            NULL DEFAULT NULL COMMENT '结束日期',
    `volunteer_hours`   DECIMAL(8,2)    NULL DEFAULT 0.00 COMMENT '志愿时长',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asp_archive_id` (`archive_id`),
    CONSTRAINT `ck_asp_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_asp_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社会实践';


-- 10. archive_book_reviews — 图书心得
CREATE TABLE `archive_book_reviews` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `archive_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联 archives.id',
    `book_name`      VARCHAR(255)    NOT NULL COMMENT '书名',
    `read_month`     DATE            NOT NULL COMMENT '阅读时间',
    `review_content` TEXT            NOT NULL COMMENT '心得体会',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_abr_archive_id` (`archive_id`),
    CONSTRAINT `fk_abr_archive_id` FOREIGN KEY (`archive_id`) REFERENCES `archives` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书心得';
