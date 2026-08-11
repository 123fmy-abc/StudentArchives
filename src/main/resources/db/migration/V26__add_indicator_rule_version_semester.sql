-- V26: indicator_rule_versions 增加 semester_id，支持按学期过滤指标树/规则版本
--
-- 背景：指标配置为学校维度（evaluation_indicators 无 semester_id），
-- 发布版本（indicator_rule_versions）此前也无学期归属，GET /admin/indicators/tree
-- 的 semesterId 仅做存在性校验、不参与过滤。
-- 本列记录每次发布版本归属的学期（null = 不限定学期），
-- 管理端按学期查询指标树时，取该学期最近发布的版本快照；未发布过则回退当前草稿树。

ALTER TABLE `indicator_rule_versions`
    ADD COLUMN `semester_id` BIGINT NULL COMMENT '发布版本归属学期（semesters.id，null=不限定学期）' AFTER `school_id`;

-- 学校 + 学期维度查询索引
CREATE INDEX idx_indicator_rule_versions_school_semester
    ON `indicator_rule_versions` (`school_id`, `semester_id`);
