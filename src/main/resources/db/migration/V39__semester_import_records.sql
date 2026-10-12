-- ============================================================
-- V39：学期批量导入记录表 semester_import_records
--
-- 背景（2026-10-10 管理员端复核问题清单 M-9）：
--   学期导入（9.6）为同步逐行校验执行、不进入任务表，导入结果只存在于当次
--   HTTP 响应里，刷新即丢、无法回看历史批次。为支撑「回看历史导入批次」，
--   新增本表落一条导入记录（含成功/失败数与失败明细），并由
--   GET /admin/semesters/import-records 提供分页查询。
-- ============================================================

CREATE TABLE `semester_import_records` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `school_id`     BIGINT UNSIGNED NOT NULL COMMENT '目标学校ID',
    `operator_id`   BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    `file_id`       BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联导入文件ID',
    `total_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    `success_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    `fail_details`  JSON            NULL DEFAULT NULL COMMENT '失败明细(JSON数组)',
    `created_at`    DATETIME        NOT NULL COMMENT '导入时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sir_school_id` (`school_id`, `id`),
    INDEX `idx_sir_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期批量导入记录表';
