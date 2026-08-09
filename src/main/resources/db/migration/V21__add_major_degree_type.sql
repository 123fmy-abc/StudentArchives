-- ============================================================
-- V21: 学生档案表新增学历层次字段
-- ============================================================

ALTER TABLE `student_profiles`
    ADD COLUMN `degree_type` VARCHAR(30) NULL DEFAULT NULL COMMENT '学历层次：associate=专科 undergraduate=本科 master=研究生/硕士 doctor=博士 postdoctor=博士后' AFTER `political_status`;
