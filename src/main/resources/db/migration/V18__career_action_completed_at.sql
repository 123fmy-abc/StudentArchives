-- ============================================================
-- V18: Add completed_at column to career_actions table
--
-- 学生端接口文档 4.12（更新行动状态）要求：status=2 时同步更新
-- career_actions.completed_at 与 completion_rate=100。
-- V8 建表时缺少 completed_at 列，本迁移补齐。
-- 使用存储过程检查列是否存在，避免重复执行时报错。
-- ============================================================

DROP PROCEDURE IF EXISTS add_career_action_completed_at_if_missing;

DELIMITER $$

CREATE PROCEDURE add_career_action_completed_at_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'career_actions'
        AND COLUMN_NAME = 'completed_at'
    ) THEN
        ALTER TABLE `career_actions`
            ADD COLUMN `completed_at` DATETIME NULL DEFAULT NULL COMMENT '完成时间（status=2 时由应用层写入）' AFTER `completion_rate`;
    END IF;
END $$

DELIMITER ;

CALL add_career_action_completed_at_if_missing();
DROP PROCEDURE add_career_action_completed_at_if_missing;
