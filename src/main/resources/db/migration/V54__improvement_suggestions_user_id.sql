-- V54: improvement_suggestions 增加 user_id 列
-- 背景：建议归属原先仅能经 weakness_id → weakness_analyses.user_id 间接溯源，
--       教师/系统生成的建议未关联短板分析时（weakness_id 为空），学生端采纳（ai-add）会 403。
-- 方案：直增 user_id 列记录归属学生，采纳校验改为 user_id 直查；存量数据按短板分析回填。

ALTER TABLE `improvement_suggestions`
    ADD COLUMN `user_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '归属学生用户 ID（users.id）' AFTER `weakness_id`,
    ADD INDEX `idx_improvement_suggestions_user_id` (`user_id`);

-- 存量回填：经 weakness_analyses 溯源归属学生
UPDATE `improvement_suggestions` s
    JOIN `weakness_analyses` w ON w.`id` = s.`weakness_id`
SET s.`user_id` = w.`user_id`
WHERE s.`user_id` IS NULL;
