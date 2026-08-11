-- V25: indicator_rule_versions 增加指标树完整快照列
--
-- 背景：evaluation_indicators.version 只是"当前版本号戳记"，发布时被整体覆盖；
-- indicator_versions 仅快照 weight/scoring_rule，无法还原完整历史指标树。
-- 本列在每次发布时写入完整指标树 JSON（含结构字段），
-- 供学生端 /common/indicators?versionId=xx 按历史版本精确查询，
-- 也作为评分计算规则版本的权威树快照。

ALTER TABLE `indicator_rule_versions`
    ADD COLUMN `tree_snapshot` JSON NULL COMMENT '发布时点的完整指标树快照（JSON，含指标结构字段），供按历史版本查询/评分追溯' AFTER `effective_at`;
