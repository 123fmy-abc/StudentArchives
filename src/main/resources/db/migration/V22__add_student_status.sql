-- ============================================================
-- V22: 学生档案表新增学生状态字段
-- ============================================================

ALTER TABLE `student_profiles`
    ADD COLUMN `student_status` VARCHAR(30) NULL DEFAULT NULL COMMENT '学生状态：current=在校生 fresh_graduate=应届毕业生 graduated=已毕业' AFTER `political_status`;
