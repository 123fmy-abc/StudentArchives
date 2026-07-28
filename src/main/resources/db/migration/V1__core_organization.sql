-- ============================================================
-- V1: Core Organization Schema
-- 基础组织架构：学校、学院、专业、班级、学期、课程、授课关系
-- ============================================================

-- 1. schools — 学校表
CREATE TABLE `schools` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100)    NOT NULL COMMENT '学校名称',
    `code`       VARCHAR(50)     NOT NULL COMMENT '学校编码',
    `status`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schools_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校表';


-- 2. colleges — 学院表
CREATE TABLE `colleges` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `name`             VARCHAR(100)    NOT NULL COMMENT '学院名称',
    `code`             VARCHAR(50)     NOT NULL COMMENT '学院编码',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_colleges_school_id` (`school_id`),
    UNIQUE KEY `uk_colleges_school_code` (`school_id`, `code`, `is_deleted_null`),
    CONSTRAINT `fk_colleges_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';


-- 3. majors — 专业表
CREATE TABLE `majors` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `college_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 colleges.id',
    `name`             VARCHAR(100)    NOT NULL COMMENT '专业名称',
    `code`             VARCHAR(50)     NOT NULL COMMENT '专业编码',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_majors_college_id` (`college_id`),
    UNIQUE KEY `uk_majors_college_code` (`college_id`, `code`, `is_deleted_null`),
    CONSTRAINT `fk_majors_college_id` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';


-- 4. classes — 班级表
CREATE TABLE `classes` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `major_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 majors.id',
    `name`           VARCHAR(100)    NOT NULL COMMENT '班级名称',
    `grade`          VARCHAR(20)     NOT NULL COMMENT '年级，如 2023级',
    `student_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '学生人数',
    `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_classes_major_id` (`major_id`),
    INDEX `idx_classes_grade` (`grade`),
    CONSTRAINT `fk_classes_major_id` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';


-- 5. semesters — 学期表
CREATE TABLE `semesters` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `name`             VARCHAR(50)     NOT NULL COMMENT '学期名称',
    `start_date`       DATE            NOT NULL COMMENT '开始日期',
    `end_date`         DATE            NOT NULL COMMENT '结束日期',
    `is_current`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否当前学期',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_semesters_is_current` (`is_current`),
    UNIQUE KEY `uk_semesters_school_name` (`school_id`, `name`, `is_deleted_null`),
    CONSTRAINT `ck_semesters_date` CHECK (`end_date` > `start_date`),
    CONSTRAINT `fk_semesters_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';


-- 6. courses — 课程主数据表
CREATE TABLE `courses` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `college_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '开课学院',
    `code`             VARCHAR(50)     NOT NULL COMMENT '课程编码',
    `name`             VARCHAR(255)    NOT NULL COMMENT '课程名称',
    `credit`           DECIMAL(3,1)    NOT NULL COMMENT '学分',
    `course_type`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '课程类型：必修/选修/实践等',
    `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_courses_school_id` (`school_id`),
    INDEX `idx_courses_college_id` (`college_id`),
    INDEX `idx_courses_status` (`status`),
    UNIQUE KEY `uk_courses_school_code` (`school_id`, `code`, `is_deleted_null`),
    CONSTRAINT `fk_courses_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_courses_college_id` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程主数据表';


-- teacher_courses 已移至 V2（依赖 users 表）
