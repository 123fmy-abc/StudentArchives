-- ============================================================
-- V53: Growth Source Score Rules — 计分规则改为按「来源记录类型」配置
--
-- 背景：event_type 只有 6 类，粒度过粗（如 event_type=3 实践同时囊括实习、
-- 社会实践、组织任职），不同类型事实对能力维度的贡献并不相同。
-- 改为按 source_type（来源记录类型，学生在关联来源时由客观记录决定，
-- 无法伪造或错选）匹配计分规则；event_type 仅保留在 growth_timelines 上
-- 用于前端分类/筛选，不再参与计分。
--
-- 规则粒度：每条规则对应一种来源记录类型，abilities JSON 元素结构：
-- [{"dimensionCode":"xxx","score":10.00}]，dimensionCode 对应
-- ability_dimensions.dimension_code（展示型引用，非外键）。
-- 无规则的来源类型：事件正常创建，但不产生能力得分行。
-- 存量 growth_timeline_abilities 得分保留不动。
-- ============================================================

CREATE TABLE `growth_source_score_rules` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `source_type`      VARCHAR(64)     NOT NULL COMMENT '来源记录类型：archives / award_applications / archive_internships 等',
    `abilities`        JSON            NOT NULL COMMENT '该类来源记录的维度得分项：[{"dimensionCode":"xxx","score":10.00}]',
    `remark`           VARCHAR(255)    NULL DEFAULT NULL COMMENT '备注',
    `created_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`       TIMESTAMP       NULL DEFAULT NULL COMMENT '软删除时间',
    `is_deleted_null`  TINYINT(1)      GENERATED ALWAYS AS (IF(`deleted_at` IS NULL, 0, NULL)) STORED NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gssr_source_type` (`source_type`, `is_deleted_null`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长事件计分规则表（按来源记录类型）';

-- 默认规则种子：维度编码对齐 ability_dimensions 字典
-- academic=学业成绩 competition=竞赛实践 practice=社会实践 quality=综合素质 moral=德育
INSERT INTO `growth_source_score_rules` (`source_type`, `abilities`, `remark`) VALUES
('archives', '[{"dimensionCode":"quality","score":5.00}]', '学生档案：完善档案基础材料'),
('award_applications', '[{"dimensionCode":"competition","score":10.00},{"dimensionCode":"quality","score":5.00}]', '奖项申报：评优获奖'),
('award_competition_stars', '[{"dimensionCode":"competition","score":15.00},{"dimensionCode":"academic","score":5.00}]', '竞赛星级：高水平竞赛获奖'),
('semester_gpa_summaries', '[{"dimensionCode":"academic","score":20.00}]', '学期GPA汇总：主学业维度'),
('gpa_records', '[{"dimensionCode":"academic","score":5.00}]', '单科GPA记录：学业过程分'),
('career_plans', '[{"dimensionCode":"quality","score":5.00}]', '职业规划：规划制定'),
('career_plan_feedbacks', '[{"dimensionCode":"quality","score":3.00},{"dimensionCode":"academic","score":2.00}]', '职业规划反馈：规划调整'),
('weakness_analyses', '[{"dimensionCode":"quality","score":5.00}]', '短板分析：自我认知'),
('improvement_suggestions', '[{"dimensionCode":"academic","score":3.00},{"dimensionCode":"practice","score":3.00}]', '改进建议：持续改进'),
('portrait_evaluation_scores', '[{"dimensionCode":"quality","score":5.00}]', '画像评分变化：阶段性提升'),
('archive_competitions', '[{"dimensionCode":"competition","score":15.00},{"dimensionCode":"academic","score":5.00}]', '竞赛经历：参赛与获奖'),
('archive_social_practices', '[{"dimensionCode":"practice","score":15.00},{"dimensionCode":"moral","score":5.00}]', '社会实践：志愿服务与社会调研'),
('archive_internships', '[{"dimensionCode":"practice","score":15.00}]', '实习经历：岗位实践'),
('archive_organizations', '[{"dimensionCode":"practice","score":10.00},{"dimensionCode":"quality","score":8.00}]', '组织任职：学生干部工作');

-- 旧表按 event_type 计分，已被本表取代
DROP TABLE IF EXISTS `growth_event_score_rules`;
