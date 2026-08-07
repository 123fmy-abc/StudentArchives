-- 个人简历导出支持：在学生档案表增加自我评价字段
ALTER TABLE student_profiles ADD COLUMN self_evaluation TEXT NULL COMMENT '自我评价' AFTER volunteer_hours;
