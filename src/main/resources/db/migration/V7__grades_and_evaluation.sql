-- ============================================================
-- V7: Grades, Evaluation, and Scoring System
-- 成绩管理、评价指标、画像评分、统计缓存
-- ============================================================

-- 1. gpa_records — 成绩/绩点表
CREATE TABLE `gpa_records` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `semester_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `course_code`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '课程编码',
    `course_name`      VARCHAR(255)    NOT NULL COMMENT '课程名称（快照）',
    `course_type`      VARCHAR(50)     NULL DEFAULT NULL COMMENT '课程类型快照',
    `attempt_no`       INT NOT NULL DEFAULT 1 COMMENT '修读次数',
    `score`            DECIMAL(5,2)    NULL DEFAULT NULL COMMENT '期末成绩',
    `gpa`              DECIMAL(3,2)    NULL DEFAULT NULL COMMENT '绩点',
    `credit`           DECIMAL(3,1)    NULL DEFAULT NULL COMMENT '学分',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_gr_school_id` (`school_id`),
    INDEX `idx_gr_course_type` (`course_type`),
    UNIQUE KEY `uk_gr_record` (`user_id`, `semester_id`, `course_code`, `attempt_no`, `is_deleted_null`),
    CONSTRAINT `ck_gr_score` CHECK (`score` >= 0 AND `score` <= 100),
    CONSTRAINT `ck_gr_gpa` CHECK (`gpa` >= 0 AND `gpa` <= 5.00),
    CONSTRAINT `fk_gr_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_gr_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_gr_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩/绩点表';


-- 2. semester_gpa_summaries — 学期成绩汇总表
CREATE TABLE `semester_gpa_summaries` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `semester_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `class_id`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '班级快照ID',
    `major_id`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '专业快照ID',
    `course_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '课程数',
    `total_credit`     DECIMAL(6,2)    NOT NULL DEFAULT 0 COMMENT '总学分',
    `weighted_gpa`     DECIMAL(3,2)    NOT NULL DEFAULT 0 COMMENT '加权平均绩点',
    `average_score`    DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '平均分',
    `rank_in_class`    INT UNSIGNED    NULL DEFAULT NULL COMMENT '班级排名',
    `rank_in_major`    INT UNSIGNED    NULL DEFAULT NULL COMMENT '专业排名',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_sgs_user_id` (`user_id`),
    INDEX `idx_sgs_class_id` (`class_id`),
    INDEX `idx_sgs_major_id` (`major_id`),
    UNIQUE KEY `uk_sgs_user_semester` (`user_id`, `semester_id`, `is_deleted_null`),
    CONSTRAINT `fk_sgs_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_sgs_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
    CONSTRAINT `fk_sgs_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_sgs_major_id` FOREIGN KEY (`major_id`) REFERENCES `majors` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期成绩汇总表';


-- 3. grade_import_logs — 成绩导入历史表
CREATE TABLE `grade_import_logs` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `operator_id`    BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    `semester_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `file_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联文件',
    `total_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    `success_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    `fail_details`   JSON            NULL DEFAULT NULL COMMENT '失败详情',
    `import_status`  INT NOT NULL DEFAULT 0 COMMENT '0=导入中 1=完成 2=失败',
    `started_at`     DATETIME        NULL DEFAULT NULL COMMENT '开始时间',
    `completed_at`   DATETIME        NULL DEFAULT NULL COMMENT '完成时间',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_gil_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩导入历史表';


-- 4. grade_import_configs — 成绩导入配置表
CREATE TABLE `grade_import_configs` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `allowed_extensions` JSON           NOT NULL COMMENT '允许上传的扩展名列表',
    `max_file_size`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '单个文件最大字节数，0=无限制',
    `template_columns`  JSON            NOT NULL COMMENT '模板列定义',
    `has_header_row`    INT NOT NULL DEFAULT 1 COMMENT '0=无表头 1=首行为表头',
    `batch_size`        INT UNSIGNED    NOT NULL DEFAULT 500 COMMENT '单批次处理行数',
    `allow_overwrite`   INT NOT NULL DEFAULT 0 COMMENT '0=追加 1=允许覆盖',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_by`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gic_school_id` (`school_id`, `is_deleted_null`),
    CONSTRAINT `fk_gic_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gic_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩导入配置表';


-- 5. user_interests — 用户兴趣
CREATE TABLE `user_interests` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `tag_name`          VARCHAR(100)    NOT NULL COMMENT '兴趣标签名',
    `proficiency_level` INT NOT NULL DEFAULT 1 COMMENT '1=入门 2=一般 3=熟练 4=精通',
    `weight`            INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '权重/出现次数',
    `sort`              INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `is_detail`         INT NOT NULL DEFAULT 0 COMMENT '0=系统标签 1=用户手动添加',
    `detail_content`    VARCHAR(255)    NULL DEFAULT NULL COMMENT '具体内容描述',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ui_user_id` (`user_id`),
    UNIQUE KEY `uk_ui_user_tag` (`user_id`, `tag_name`, `is_detail`, `is_deleted_null`),
    CONSTRAINT `fk_ui_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣';


-- 6. award_summaries — 个人奖项汇总表
CREATE TABLE `award_summaries` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `category`         VARCHAR(50)     NOT NULL COMMENT '类别',
    `total_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总次数',
    `max_level`        VARCHAR(50)     NULL DEFAULT NULL COMMENT '最高级别',
    `latest_at`        DATE            NULL DEFAULT NULL COMMENT '最近一次时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_aws_user_id` (`user_id`),
    UNIQUE KEY `uk_aws_user_category` (`user_id`, `category`, `is_deleted_null`),
    CONSTRAINT `fk_aws_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人奖项汇总表';


-- 7. ability_dimensions — 能力维度字典表
CREATE TABLE `ability_dimensions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `dimension_name`   VARCHAR(50)     NOT NULL COMMENT '维度名称',
    `dimension_code`   VARCHAR(50)     NOT NULL COMMENT '维度编码',
    `description`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '维度描述',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ad_status` (`status`),
    UNIQUE KEY `uk_ad_dimension_code` (`dimension_code`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='能力维度字典表';


-- 8. evaluation_indicators — 评价指标表
CREATE TABLE `evaluation_indicators` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `indicator_name`   VARCHAR(100)    NOT NULL COMMENT '指标名称',
    `indicator_code`   VARCHAR(50)     NOT NULL COMMENT '指标编码',
    `parent_id`        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '父级指标 ID',
    `level`            INT NOT NULL DEFAULT 1 COMMENT '层级：1/2/3',
    `path`             VARCHAR(255)    NULL DEFAULT NULL COMMENT '指标路径，如 001.002.003',
    `weight`           DECIMAL(5,4)    NOT NULL DEFAULT 0 COMMENT '权重（如 0.2500）',
    `scoring_rule`     JSON            NULL DEFAULT NULL COMMENT '计分规则配置',
    `dimension_code`   VARCHAR(50)     NULL DEFAULT NULL COMMENT '关联 ability_dimensions.dimension_code',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `version`          INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ei_parent_id` (`parent_id`),
    INDEX `idx_ei_dimension_code` (`dimension_code`),
    INDEX `idx_ei_status` (`status`),
    UNIQUE KEY `uk_ei_code` (`school_id`, `indicator_code`, `is_deleted_null`),
    CONSTRAINT `fk_ei_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ei_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `evaluation_indicators` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标表';


-- 9. indicator_versions — 指标版本历史表
CREATE TABLE `indicator_versions` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `indicator_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 evaluation_indicators.id',
    `version`         INT UNSIGNED    NOT NULL COMMENT '版本号',
    `weight`          DECIMAL(5,4)    NOT NULL COMMENT '该版本权重',
    `scoring_rule`    JSON            NULL DEFAULT NULL COMMENT '该版本计分规则',
    `change_summary`  VARCHAR(255)    NULL DEFAULT NULL COMMENT '变更说明',
    `created_by`      BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_iv_indicator_version` (`indicator_id`, `version`),
    INDEX `idx_iv_indicator_id` (`indicator_id`),
    CONSTRAINT `fk_iv_indicator_id` FOREIGN KEY (`indicator_id`) REFERENCES `evaluation_indicators` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标版本历史表';


-- 10. indicator_rule_versions — 指标规则快照版本表
CREATE TABLE `indicator_rule_versions` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `version`          INT UNSIGNED    NOT NULL COMMENT '全局规则快照版本号',
    `version_name`     VARCHAR(100)    NULL DEFAULT NULL COMMENT '版本名称',
    `effective_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    `created_by`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '创建人 ID',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_irv_school_version` (`school_id`, `version`, `is_deleted_null`),
    CONSTRAINT `fk_irv_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_irv_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标规则快照版本表';


-- 11. score_calculations — 评分计算批次表
CREATE TABLE `score_calculations` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联 users.id',
    `semester_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `calculated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算执行时间',
    `rule_version`     INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '所使用的指标规则版本号',
    `data_source`      VARCHAR(255)    NULL DEFAULT NULL COMMENT '数据来源说明',
    `trigger_type`     INT NOT NULL DEFAULT 1 COMMENT '1=手动触发 2=系统自动/定时任务',
    `operator_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '操作人 ID',
    `status` INT NOT NULL DEFAULT 1 COMMENT '0=计算中 1=完成 2=失败',
    `lock_version`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sc_user_semester` (`user_id`, `semester_id`, `calculated_at`),
    INDEX `idx_sc_trigger_type` (`trigger_type`),
    INDEX `idx_sc_status` (`status`),
    CONSTRAINT `fk_sc_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_sc_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
    CONSTRAINT `fk_sc_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分计算批次表';


-- 12. score_calculation_details — 评分计算明细表
CREATE TABLE `score_calculation_details` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `calculation_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 score_calculations.id',
    `indicator_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 evaluation_indicators.id',
    `dimension_code`    VARCHAR(50)     NOT NULL COMMENT '能力维度编码',
    `raw_score`         DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '原始得分',
    `weight`            DECIMAL(5,4)    NOT NULL DEFAULT 0 COMMENT '该指标权重',
    `weighted_score`    DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '加权后得分',
    `source_archive_ids` JSON           NULL DEFAULT NULL COMMENT '来源档案 ID 列表',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_scd_calculation_id` (`calculation_id`),
    INDEX `idx_scd_indicator_id` (`indicator_id`),
    CONSTRAINT `fk_scd_calculation_id` FOREIGN KEY (`calculation_id`) REFERENCES `score_calculations` (`id`),
    CONSTRAINT `fk_scd_indicator_id` FOREIGN KEY (`indicator_id`) REFERENCES `evaluation_indicators` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分计算明细表';


-- 13. portrait_evaluation_scores — 画像评估得分表
CREATE TABLE `portrait_evaluation_scores` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `semester_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `calculation_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 score_calculations.id',
    `dimension_code`      VARCHAR(50)     NOT NULL COMMENT '维度编码',
    `score`               DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '当前得分',
    `target_score`        DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '目标分',
    `change`              DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '较上阶段变化',
    `gap`                 DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '距目标分数',
    `compared_semester_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '较上阶段对比学期 ID',
    `rule_version`        INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '计算所使用的指标规则版本号',
    `evaluated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    `created_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`          TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`     TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_pes_user_id` (`user_id`),
    INDEX `idx_pes_calculation_id` (`calculation_id`),
    INDEX `idx_pes_compared_semester_id` (`compared_semester_id`),
    UNIQUE KEY `uk_pes_user_semester_dimension` (`user_id`, `semester_id`, `dimension_code`, `is_deleted_null`),
    CONSTRAINT `fk_pes_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_pes_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像评估得分表';


-- 14. data_completeness — 数据完整度表
CREATE TABLE `data_completeness` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '学生 ID',
    `semester_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 semesters.id',
    `dimension_code`    VARCHAR(50)     NOT NULL COMMENT '维度编码',
    `completeness_rate` INT NOT NULL DEFAULT 0 COMMENT '完整度 0-100',
    `missing_items`     JSON            NULL DEFAULT NULL COMMENT '缺失项列表',
    `calculated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    `created_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`   TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_dc_user_id` (`user_id`),
    UNIQUE KEY `uk_dc_user_semester_dimension` (`user_id`, `semester_id`, `dimension_code`, `is_deleted_null`),
    CONSTRAINT `fk_dc_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_dc_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据完整度表';


-- 15. org_archive_summaries — 组织档案汇总快照表
CREATE TABLE `org_archive_summaries` (
    `id`                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`                 BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `semester_id`               BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `stat_date`                 DATE            NOT NULL COMMENT '统计日期',
    `org_type`                  INT NOT NULL COMMENT '1=学校 2=学院 3=专业 4=班级',
    `org_id`                    BIGINT UNSIGNED NOT NULL COMMENT '对应组织表 ID',
    `grade`                     VARCHAR(20)     NULL DEFAULT NULL COMMENT '年级',
    `total_students`            INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '学生总数',
    `total_archives`            INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '档案总数',
    `total_awards`              INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '获奖总数',
    `avg_gpa`                   DECIMAL(3,2)    NULL DEFAULT NULL COMMENT '平均绩点',
    `top_dimensions`            JSON            NULL DEFAULT NULL COMMENT 'TOP5 能力维度 JSON',
    `hot_tags`                  JSON            NULL DEFAULT NULL COMMENT '热门兴趣标签 JSON',
    `archive_type_distribution` JSON            NULL DEFAULT NULL COMMENT '各档案类型数量分布',
    `created_at`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`                TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`           TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oas_unique` (`school_id`, `semester_id`, `org_type`, `org_id`, `stat_date`, `is_deleted_null`),
    CONSTRAINT `fk_oas_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_oas_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织档案汇总快照表';


-- 16. score_recalculation_tasks — 评分重新计算任务表
CREATE TABLE `score_recalculation_tasks` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `task_type`      INT NOT NULL COMMENT '1=指定学生 2=指定班级 3=指定学期 4=全量 5=指定专业',
    `target_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '目标ID',
    `semester_id`    BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '指定学期时填写',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0=待执行 1=执行中 2=完成 3=失败',
    `triggered_by`   BIGINT UNSIGNED NOT NULL COMMENT '触发人ID',
    `triggered_at`   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    `started_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '开始执行时间',
    `completed_at`   TIMESTAMP       NULL DEFAULT NULL COMMENT '完成时间',
    `total_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '待计算记录总数',
    `success_count`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功计算数',
    `fail_count`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败计算数',
    `progress`       INT NOT NULL DEFAULT 0 COMMENT '进度百分比 0~100',
    `error_message`  TEXT            NULL DEFAULT NULL COMMENT '错误信息',
    `lock_version`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`     TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_srt_school_id` (`school_id`),
    INDEX `idx_srt_task_type` (`task_type`),
    INDEX `idx_srt_status` (`status`),
    INDEX `idx_srt_target_id` (`target_id`),
    INDEX `idx_srt_semester_id` (`semester_id`),
    CONSTRAINT `fk_srt_school_id` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`),
    CONSTRAINT `fk_srt_semester_id` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
    CONSTRAINT `fk_srt_triggered_by` FOREIGN KEY (`triggered_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分重新计算任务表';


-- 17. statistics_cache — 统计缓存表
CREATE TABLE `statistics_cache` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`        BIGINT UNSIGNED NOT NULL COMMENT '关联 schools.id',
    `cache_key`        VARCHAR(255)    NOT NULL COMMENT '缓存标识',
    `scope_type`       INT NULL DEFAULT NULL COMMENT '1=全校 2=学院 3=专业 4=班级',
    `scope_id`         BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '对应组织表 ID',
    `semester_id`      BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联 semesters.id',
    `stat_type`        VARCHAR(50)     NOT NULL COMMENT '统计类型：archive/award/gpa 等',
    `stat_data`        JSON            NOT NULL COMMENT '缓存的统计结果 JSON',
    `expired_at`       DATETIME        NOT NULL COMMENT '缓存失效时间',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_sc_expired_at` (`expired_at`),
    INDEX `idx_sc_scope` (`scope_type`, `scope_id`),
    INDEX `idx_sc_semester_id` (`semester_id`),
    UNIQUE KEY `uk_sc_cache_key` (`school_id`, `cache_key`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计缓存表';


-- 18. user_favorites — 用户收藏/快捷入口表
CREATE TABLE `user_favorites` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '关联用户',
    `favorite_type`    VARCHAR(50)     NOT NULL COMMENT '收藏类型',
    `target_id`        VARCHAR(100)    NOT NULL COMMENT '目标标识',
    `target_name`      VARCHAR(100)    NOT NULL COMMENT '目标名称',
    `icon`             VARCHAR(100)    NULL DEFAULT NULL COMMENT '图标类名',
    `sort`             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '排序',
    `last_used_at`     DATETIME        NULL DEFAULT NULL COMMENT '最近使用时间',
    `use_count`        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '使用次数',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_uf_user_id` (`user_id`),
    UNIQUE KEY `uk_uf_favorite` (`user_id`, `favorite_type`, `target_id`, `is_deleted_null`),
    CONSTRAINT `fk_uf_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏/快捷入口表';
