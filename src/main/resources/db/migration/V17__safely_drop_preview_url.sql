-- ============================================================
-- V17: Safely drop preview_url column from file_uploads table (consolidated)
--
-- 合并 V15/V16，安全删除 file_uploads 表中已废弃的 preview_url 列。
-- 使用存储过程检查列是否存在，避免列不存在时 ALTER TABLE 报错。
--
-- 背景：V15 和 V16 已从版本库移除，由本迁移替代。
-- ============================================================

DROP PROCEDURE IF EXISTS drop_preview_url_if_exists;

DELIMITER $$

CREATE PROCEDURE drop_preview_url_if_exists()
BEGIN
    IF EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'file_uploads'
        AND COLUMN_NAME = 'preview_url'
    ) THEN
        ALTER TABLE `file_uploads` DROP COLUMN `preview_url`;
    END IF;
END $$

DELIMITER ;

CALL drop_preview_url_if_exists();
DROP PROCEDURE drop_preview_url_if_exists;
