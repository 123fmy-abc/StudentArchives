-- ============================================================
-- V52: Seed Growth Event Score Rules — 成长事件计分规则种子数据
-- 为 6 种事件类型各预置一条合理默认规则，管理员可在
-- /admin/growth-event-score-rules 页面调整。
--
-- 注意：abilities.dimensionCode 为展示型引用（非外键），种子按文档示例口径
-- 使用 academic（学业能力）/ practice（实践能力）/ innovation（创新能力）；
-- 若学校 ability_dimensions 字典编码不同，请通过管理端修改规则。
-- 分值语义：单条该类事件对维度的得分贡献（0-100），画像按事件累加。
-- ============================================================

INSERT INTO `growth_event_score_rules` (`event_type`, `abilities`, `remark`) VALUES
(1, '[{"dimensionCode":"practice","score":15.00},{"dimensionCode":"innovation","score":10.00},{"dimensionCode":"academic","score":10.00}]', '奖项：竞赛/评优获奖，偏实践与创新'),
(2, '[{"dimensionCode":"academic","score":20.00}]', '成绩： GPA/课程成绩，主学业维度'),
(3, '[{"dimensionCode":"practice","score":15.00}]', '实践：实习/社会实践/组织任职'),
(4, '[{"dimensionCode":"academic","score":5.00},{"dimensionCode":"innovation","score":5.00}]', '职业规划：规划与反馈，轻量加分'),
(5, '[{"dimensionCode":"academic","score":5.00},{"dimensionCode":"practice","score":5.00},{"dimensionCode":"innovation","score":5.00}]', '短板改进：均衡小分，鼓励持续改进'),
(6, '[{"dimensionCode":"practice","score":10.00},{"dimensionCode":"innovation","score":10.00}]', '能力提升：画像评分变化事件');
