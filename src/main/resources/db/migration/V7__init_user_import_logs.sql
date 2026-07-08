-- ============================================================================
-- V7: 用户导入日志表（User Import Logs）
-- 描述：记录用户批量导入的历史和结果，与 grade_import_logs 对称
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 7.1 用户导入日志表
-- ----------------------------------------------------------------------------
CREATE TABLE user_import_logs (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    school_id      BIGINT UNSIGNED NOT NULL COMMENT '目标学校ID',
    operator_id    BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    file_id        BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '关联导入文件',
    total_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '总记录数',
    success_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '成功数',
    fail_count     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '失败数',
    fail_details   JSON            NULL DEFAULT NULL COMMENT '失败详情：[{"row":5,"userNo":"...","reason":"学号已存在"}]',
    import_status  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=导入中 1=完成 2=失败',
    started_at     TIMESTAMP       NULL DEFAULT NULL COMMENT '开始时间',
    completed_at   TIMESTAMP       NULL DEFAULT NULL COMMENT '完成时间',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP       NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_import_logs_school (school_id),
    INDEX idx_user_import_logs_operator (operator_id),
    INDEX idx_user_import_logs_status (import_status),
    INDEX idx_user_import_logs_created (created_at),
    CONSTRAINT fk_user_import_logs_school FOREIGN KEY (school_id) REFERENCES schools(id),
    CONSTRAINT fk_user_import_logs_operator FOREIGN KEY (operator_id) REFERENCES users(id),
    CONSTRAINT fk_user_import_logs_file FOREIGN KEY (file_id) REFERENCES file_uploads(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户导入日志表';

-- 注：关联文件被软删除后，查询时需检查 file_uploads.deleted_at，避免展示 404
