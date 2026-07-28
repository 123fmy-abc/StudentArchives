-- ============================================================
-- V2: Users and Authentication
-- 用户、角色、权限、组织范围、个人档案扩展
-- ============================================================

-- 1. users — 用户表
CREATE TABLE `users` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id，多校数据隔离',
    `user_no`          VARCHAR(50)     NOT NULL COMMENT '统一编号：学号/工号/管理员工号',
    `name`             VARCHAR(100)    NOT NULL COMMENT '姓名',
    `gender`           INT NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    `birth_date`       DATE            NULL DEFAULT NULL COMMENT '出生日期',
    `password`         VARCHAR(255)    NOT NULL COMMENT 'Bcrypt哈希',
    `remember_token`   VARCHAR(100)    NULL DEFAULT NULL COMMENT '记住我令牌',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    `token_version`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '令牌版本号',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_users_status` (`status`),
    UNIQUE KEY `uk_users_school_no` (`school_id`, `user_no`, `is_deleted_null`),
    CONSTRAINT `fk_users_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- 2. roles — 角色表
CREATE TABLE `roles` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(50)     NOT NULL COMMENT '角色名称（学校自定义）',
    `code`             VARCHAR(50)     NOT NULL COMMENT '角色编码（系统生成，全局唯一）',
    `description`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '角色描述',
    `level`            INT NOT NULL DEFAULT 0 COMMENT '0=系统 1=学生 2=教师 3=辅导员 4=系主任 5=院长 6=校长 7+=自定义',
    `role_type`        INT NOT NULL DEFAULT 1 COMMENT '1=教学类 2=行政类 3=审核类 4=系统管理类',
    `is_system`        INT NOT NULL DEFAULT 0 COMMENT '0=学校自定义 1=系统内置（不可删除）',
    `is_auditor`       INT NOT NULL DEFAULT 0 COMMENT '0=非审批角色 1=可作为审批节点',
    `scope_types`      JSON            NULL DEFAULT NULL COMMENT '允许绑定的范围类型：[2,3,4,5]',
    `max_scope_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '最大绑定数量：0=无限制',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_roles_level` (`level`),
    INDEX `idx_roles_system_status` (`is_system`, `status`),
    INDEX `idx_roles_auditor_status` (`is_auditor`, `status`),
    INDEX `idx_roles_role_type` (`role_type`),
    UNIQUE KEY `uk_roles_code` (`code`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';


-- 3. user_roles — 用户角色关联表
CREATE TABLE `user_roles` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `role_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_roles` (`user_id`, `role_id`, `is_deleted_null`),
    CONSTRAINT `fk_user_roles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_roles_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';


-- 4. permissions — 权限表
CREATE TABLE `permissions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(100)    NOT NULL COMMENT '权限名称',
    `code`             VARCHAR(100)    NOT NULL COMMENT '权限编码（全局唯一）',
    `type`             INT NOT NULL DEFAULT 1 COMMENT '1=menu菜单 2=button按钮 3=api接口',
    `parent_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '父权限ID',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_permissions_parent_id` (`parent_id`),
    UNIQUE KEY `uk_permissions_code` (`code`, `is_deleted_null`),
    CONSTRAINT `fk_permissions_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `permissions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';


-- 5. role_permissions — 角色权限关联表
CREATE TABLE `role_permissions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    `permission_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 permissions.id',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permissions` (`role_id`, `permission_id`, `is_deleted_null`),
    CONSTRAINT `fk_role_permissions_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permissions_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';


-- 6. role_scopes — 角色组织范围绑定表
CREATE TABLE `role_scopes` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id，冗余字段方便查询',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `role_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 roles.id',
    `scope_type`       INT NOT NULL COMMENT '范围类型：1=学校 2=学院 3=专业 4=班级 5=课程 6=年级',
    `scope_id`         BIGINT UNSIGNED NOT NULL COMMENT '对应组织表ID',
    `semester_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联学期，支持按学期绑定权限',
    `is_primary`       INT NOT NULL DEFAULT 1 COMMENT '0=兼任 1=主职',
    `appoint_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '任命人ID（系统管理员）',
    `appoint_reason`   VARCHAR(255)    NULL DEFAULT NULL COMMENT '任命原因/备注',
    `valid_from`       DATE            NOT NULL COMMENT '生效日期',
    `valid_until`      DATE            NULL DEFAULT NULL COMMENT '失效日期（空=永久）',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用 2=过期',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_role_scopes_school_id` (`school_id`),
    UNIQUE KEY `uk_role_scopes_assign` (`user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`, `is_deleted_null`),
    CONSTRAINT `fk_role_scopes_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_scopes_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_scopes_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_scopes_appoint_by` FOREIGN KEY (`appoint_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_role_scopes_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色组织范围绑定表';


-- 7. student_profiles — 学生档案扩展表
CREATE TABLE `student_profiles` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `class_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 classes.id（当前所在班级）',
    `political_status` VARCHAR(50)     NULL DEFAULT NULL COMMENT '政治面貌',
    `volunteer_hours`  DECIMAL(8,2)    NULL DEFAULT 0.00 COMMENT '志愿时长汇总',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_profiles_user_id` (`user_id`),
    INDEX `idx_student_profiles_class_id` (`class_id`),
    CONSTRAINT `fk_student_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_student_profiles_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生档案扩展表';


-- 8. teacher_profiles — 教师档案扩展表
CREATE TABLE `teacher_profiles` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `college_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '所属学院',
    `title`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '职称',
    `created_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_profiles_user_id` (`user_id`),
    INDEX `idx_teacher_profiles_college_id` (`college_id`),
    CONSTRAINT `fk_teacher_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_teacher_profiles_college_id` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师档案扩展表';


-- 9. user_contact_infos — 用户联系信息表
CREATE TABLE `user_contact_infos` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `phone`             VARCHAR(20)     NULL DEFAULT NULL COMMENT '手机号',
    `email`             VARCHAR(255)    NULL DEFAULT NULL COMMENT '邮箱',
    `avatar`            VARCHAR(255)    NULL DEFAULT NULL COMMENT '头像 URL',
    `address`           VARCHAR(255)    NULL DEFAULT NULL COMMENT '通讯地址',
    `emergency_name`    VARCHAR(50)     NULL DEFAULT NULL COMMENT '紧急联系人姓名',
    `emergency_relation` VARCHAR(30)   NULL DEFAULT NULL COMMENT '与紧急联系人关系',
    `emergency_phone`   VARCHAR(20)     NULL DEFAULT NULL COMMENT '紧急联系人电话',
    `updated_by`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '最后修改人 ID',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_contact_infos_user_id` (`user_id`, `is_deleted_null`),
    CONSTRAINT `fk_user_contact_infos_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_contact_infos_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户联系信息表';


-- 10. teacher_courses — 教师授课关系表
CREATE TABLE `teacher_courses` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `teacher_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id（教师）',
    `class_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 classes.id',
    `course_code`      VARCHAR(50)     NOT NULL COMMENT '课程编码',
    `course_name`      VARCHAR(255)    NOT NULL COMMENT '课程名称',
    `semester_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `is_primary`       INT NOT NULL DEFAULT 1 COMMENT '0=兼任 1=主职',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_courses_semester_id` (`semester_id`),
    INDEX `idx_teacher_courses_course_code` (`course_code`),
    UNIQUE KEY `uk_teacher_courses_assign` (`teacher_id`, `class_id`, `course_code`, `semester_id`, `is_deleted_null`),
    CONSTRAINT `fk_tc_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_tc_teacher_id` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_tc_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
    CONSTRAINT `fk_tc_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师授课关系表';
