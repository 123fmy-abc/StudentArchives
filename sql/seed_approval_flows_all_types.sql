-- ============================================================
-- 为每一类申报补齐审批流程（以「学科竞赛」流程为模板克隆审批节点）
--
-- 背景：
--   当前只有「学科竞赛」（Archive + academic_competition）配置了审批流程，
--   其余档案类型、奖项类型、职业规划的提交会因命中不到流程而抛
--   APPROVAL_TASK_CREATE_FAILED（提交整体回滚）。
--
-- 做法：
--   1. 按学校取出「学科竞赛」实际生效的流程（优先取 approval_flow_mappings
--      映射的流程，未配置映射时回退到 applicable_sub_type='academic_competition'
--      的启用流程）；
--   2. 为每个学校补齐 13 条流程（9 类档案 + 3 类奖项 + 职业规划），
--      步骤（辅导员 → 系主任 → 院长）逐字段从学科竞赛流程克隆，节点完全一致；
--   3. 为补齐的流程生成 business_type + business_sub_type 映射（is_default=1）。
--
-- 特性：
--   - 幂等：已存在的流程（school_id + 类型 + 子类型 + version=1）不重复创建；
--     流程已存在但缺少步骤时，仅补步骤；映射已存在时不重复创建；
--   - 不修改「学科竞赛」原有流程/映射；
--   - 无 DDL 结构变更，仅数据写入；未使用迁移文件，手工执行本脚本即可。
--
-- 执行：mysql -h <host> -u <user> -p <db> < sql/seed_approval_flows_all_types.sql
-- 回滚：见文件末尾「回滚」段（按流程名 + copied_from 定位本次生成的数据）。
-- ============================================================

-- ------------------------------------------------------------
-- 0. 收集模板流程（每个学校一条）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_src_flow;
CREATE TEMPORARY TABLE tmp_src_flow (
    `school_id`  BIGINT UNSIGNED NOT NULL,
    `flow_id`    BIGINT UNSIGNED NOT NULL,
    `created_by` BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`school_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 0.1 优先：学科竞赛已配置映射时，取映射指向的启用流程
--     注意：MySQL 临时表不允许在同一语句中被重复打开，
--     故这里先聚合出每校最小映射 ID，再回连映射表与流程表，不直接引用 tmp_src_flow。
INSERT INTO tmp_src_flow (`school_id`, `flow_id`, `created_by`)
SELECT k.`school_id`, f.`id`, f.`created_by`
FROM (
    SELECT m.`school_id`, MIN(m.`id`) AS `mapping_id`
    FROM `approval_flow_mappings` m
    JOIN `approval_flows` f0
      ON f0.`id` = m.`flow_id`
     AND f0.`deleted_at` IS NULL
     AND f0.`status` = 1
    WHERE m.`deleted_at` IS NULL
      AND m.`business_type` = 'Archive'
      AND m.`business_sub_type` = 'academic_competition'
    GROUP BY m.`school_id`
) k
JOIN `approval_flow_mappings` mm ON mm.`id` = k.`mapping_id`
JOIN `approval_flows` f
  ON f.`id` = mm.`flow_id`
 AND f.`deleted_at` IS NULL
 AND f.`status` = 1;

-- 0.2 兜底：未配置学科竞赛映射，但存在子类型为 academic_competition 的启用流程
INSERT INTO tmp_src_flow (`school_id`, `flow_id`, `created_by`)
SELECT f.`school_id`, f.`id`, f.`created_by`
FROM `approval_flows` f
JOIN (
    SELECT `school_id`, MIN(`id`) AS `id`
    FROM `approval_flows`
    WHERE `deleted_at` IS NULL
      AND `status` = 1
      AND `applicable_type` = 'Archive'
      AND `applicable_sub_type` = 'academic_competition'
    GROUP BY `school_id`
) k ON k.`id` = f.`id`
WHERE NOT EXISTS (
    SELECT 1
    FROM `approval_flow_mappings` m
    WHERE m.`school_id` = f.`school_id`
      AND m.`deleted_at` IS NULL
      AND m.`business_type` = 'Archive'
      AND m.`business_sub_type` = 'academic_competition'
);

-- ------------------------------------------------------------
-- 1. 目标申报类型清单（13 类，均与学科竞赛共用同一套审批节点）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_target;
CREATE TEMPORARY TABLE tmp_target (
    `applicable_type`     VARCHAR(50)  NOT NULL,
    `applicable_sub_type` VARCHAR(50)  NULL,
    `flow_name`           VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_target (`applicable_type`, `applicable_sub_type`, `flow_name`) VALUES
-- 档案类（Archive），子类型 = archive_type（学科竞赛已存在，不重复创建）
('Archive', 'innovation_entrepreneurship', '创新创业申报审批流程'),
('Archive', 'academic_research',           '学术研究申报审批流程'),
('Archive', 'scholarship',                 '奖学金申报审批流程'),
('Archive', 'honor_certificate',           '荣誉证书申报审批流程'),
('Archive', 'internship',                  '实习经历申报审批流程'),
('Archive', 'organization',                '组织履历申报审批流程'),
('Archive', 'training_project',            '实训项目申报审批流程'),
('Archive', 'social_practice',             '社会实践申报审批流程'),
('Archive', 'book_review',                 '图书心得申报审批流程'),
-- 奖项类（AwardApplication），子类型 = award_type
('AwardApplication', 'competition_star',   '竞赛之星申报审批流程'),
('AwardApplication', 'research_star',      '科研之星申报审批流程'),
('AwardApplication', 'innovation_star',    '双创之星申报审批流程'),
-- 职业规划（CareerPlan，无子类型）
('CareerPlan', NULL,                       '职业生涯规划审批流程');

-- ------------------------------------------------------------
-- 2. 创建流程主表记录
--    识别口径：flow_name = tmp_target.flow_name（本次生成的专用流程名）。
--    不能只按「同类型 + 同子类型」判断是否已存在：职业规划已有
--    「默认审批流程」（applicable_sub_type 为 NULL、仅 1 步管理员审核），
--    按类型判断会误认为已配置而跳过，导致职业规划仍走 1 步流程。
--    版本号取该 (学校, 类型, 子类型) 现有最大版本 + 1，避免唯一索引冲突。
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_next_version;
CREATE TEMPORARY TABLE tmp_next_version (
    `school_id`           BIGINT UNSIGNED NOT NULL,
    `applicable_type`     VARCHAR(50)     NOT NULL,
    `applicable_sub_type` VARCHAR(50)     NULL,
    `next_version`        INT UNSIGNED    NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_next_version
    (`school_id`, `applicable_type`, `applicable_sub_type`, `next_version`)
SELECT `school_id`, `applicable_type`, `applicable_sub_type`, MAX(`version`) + 1
FROM `approval_flows`
WHERE `deleted_at` IS NULL
GROUP BY `school_id`, `applicable_type`, `applicable_sub_type`;

START TRANSACTION;

INSERT INTO `approval_flows`
    (`school_id`, `flow_name`, `applicable_type`, `applicable_sub_type`,
     `version`, `copied_from`, `is_default`, `status`, `created_by`,
     `created_at`, `updated_at`)
SELECT s.`school_id`, t.`flow_name`, t.`applicable_type`, t.`applicable_sub_type`,
       COALESCE(v.`next_version`, 1), s.`flow_id`, 0, 1, s.`created_by`,
       NOW(), NOW()
FROM tmp_src_flow s
CROSS JOIN tmp_target t
LEFT JOIN tmp_next_version v
       ON v.`school_id` = s.`school_id`
      AND v.`applicable_type` = t.`applicable_type`
      AND v.`applicable_sub_type` <=> t.`applicable_sub_type`
WHERE NOT EXISTS (
    SELECT 1
    FROM `approval_flows` f
    WHERE f.`school_id` = s.`school_id`
      AND f.`applicable_type` = t.`applicable_type`
      AND f.`applicable_sub_type` <=> t.`applicable_sub_type`
      AND f.`flow_name` = t.`flow_name`
      AND f.`deleted_at` IS NULL
);

-- ------------------------------------------------------------
-- 3. 克隆审批节点（仅为目标流程当前「无任何步骤」的情况补步骤，
--    已配置过步骤的流程保持原样，避免覆盖人工调整）
-- ------------------------------------------------------------
INSERT INTO `approval_flow_steps`
    (`flow_id`, `step_no`, `step_name`, `role_id`, `scope_type`, `scope_rule`,
     `auto_assign`, `allow_delegate`, `allow_skip`, `allow_designate_next`,
     `timeout_hours`, `reject_action`, `reject_to_step`, `sort`,
     `created_at`, `updated_at`)
SELECT f.`id`, src.`step_no`, src.`step_name`, src.`role_id`,
       src.`scope_type`, src.`scope_rule`,
       src.`auto_assign`, src.`allow_delegate`, src.`allow_skip`,
       src.`allow_designate_next`, src.`timeout_hours`,
       src.`reject_action`, src.`reject_to_step`, src.`sort`,
       NOW(), NOW()
FROM `approval_flows` f
JOIN tmp_src_flow fs ON fs.`school_id` = f.`school_id`
JOIN `approval_flow_steps` src
  ON src.`flow_id` = fs.`flow_id`
 AND src.`deleted_at` IS NULL
JOIN tmp_target t
  ON t.`applicable_type` = f.`applicable_type`
 AND t.`applicable_sub_type` <=> f.`applicable_sub_type`
WHERE f.`deleted_at` IS NULL
  AND f.`flow_name` = t.`flow_name`
  AND NOT EXISTS (
      SELECT 1
      FROM `approval_flow_steps` x
      WHERE x.`flow_id` = f.`id`
        AND x.`deleted_at` IS NULL
  );

-- ------------------------------------------------------------
-- 4. 生成/重指向业务映射（business_type + business_sub_type → flow_id）
--    4.1 已有映射但指向旧流程（如职业规划的「默认审批流程」）：改指本次专用流程；
--    4.2 无映射：新建映射（is_default=1）。
--    注意：business_sub_type 为 NULL 时唯一索引不会判重（NULL <> NULL），
--    因此不能依赖 INSERT ... ON DUPLICATE KEY，必须先 UPDATE 再补 INSERT。
-- ------------------------------------------------------------
UPDATE `approval_flow_mappings` m
JOIN `approval_flows` f
  ON f.`school_id` = m.`school_id`
 AND f.`applicable_type` = m.`business_type`
 AND f.`applicable_sub_type` <=> m.`business_sub_type`
JOIN tmp_target t
  ON t.`applicable_type` = f.`applicable_type`
 AND t.`applicable_sub_type` <=> f.`applicable_sub_type`
SET m.`flow_id` = f.`id`,
    m.`is_default` = 1,
    m.`priority` = 10,
    m.`updated_at` = NOW()
WHERE m.`deleted_at` IS NULL
  AND f.`deleted_at` IS NULL
  AND f.`status` = 1
  AND f.`flow_name` = t.`flow_name`
  AND m.`flow_id` <> f.`id`;

INSERT INTO `approval_flow_mappings`
    (`school_id`, `business_type`, `business_sub_type`, `flow_id`,
     `is_default`, `priority`, `created_by`, `created_at`, `updated_at`)
SELECT f.`school_id`, f.`applicable_type`, f.`applicable_sub_type`, f.`id`,
       1, 10, f.`created_by`, NOW(), NOW()
FROM `approval_flows` f
JOIN tmp_target t
  ON t.`applicable_type` = f.`applicable_type`
 AND t.`applicable_sub_type` <=> f.`applicable_sub_type`
WHERE f.`deleted_at` IS NULL
  AND f.`status` = 1
  AND f.`flow_name` = t.`flow_name`
  AND NOT EXISTS (
      SELECT 1
      FROM `approval_flow_mappings` m
      WHERE m.`school_id` = f.`school_id`
        AND m.`business_type` = f.`applicable_type`
        AND m.`business_sub_type` <=> f.`applicable_sub_type`
        AND m.`deleted_at` IS NULL
  );

COMMIT;

-- ------------------------------------------------------------
-- 5. 验证
-- ------------------------------------------------------------
-- 5.1 模板流程来源（每个学校一条；为空说明该校没有「学科竞赛」流程，需先配置）
SELECT s.`school_id`, s.`flow_id`, f.`flow_name`,
       (SELECT COUNT(*) FROM `approval_flow_steps` x
        WHERE x.`flow_id` = s.`flow_id` AND x.`deleted_at` IS NULL) AS `src_step_count`
FROM tmp_src_flow s
JOIN `approval_flows` f ON f.`id` = s.`flow_id`;

-- 5.2 每个学校每类申报的最终配置（flow + 步骤数 + 映射）
SELECT f.`school_id`,
       f.`applicable_type`   AS `business_type`,
       f.`applicable_sub_type` AS `business_sub_type`,
       f.`flow_name`,
       f.`id` AS `flow_id`,
       (SELECT COUNT(*) FROM `approval_flow_steps` x
        WHERE x.`flow_id` = f.`id` AND x.`deleted_at` IS NULL) AS `step_count`,
       (SELECT GROUP_CONCAT(CONCAT(x.`step_no`, ':', x.`step_name`) ORDER BY x.`step_no`)
        FROM `approval_flow_steps` x
        WHERE x.`flow_id` = f.`id` AND x.`deleted_at` IS NULL) AS `steps`,
       (SELECT COUNT(*) FROM `approval_flow_mappings` m
        WHERE m.`flow_id` = f.`id` AND m.`deleted_at` IS NULL) AS `mapping_count`
FROM `approval_flows` f
JOIN tmp_target t
  ON t.`applicable_type` = f.`applicable_type`
 AND t.`applicable_sub_type` <=> f.`applicable_sub_type`
WHERE f.`deleted_at` IS NULL
  AND f.`flow_name` = t.`flow_name`
ORDER BY f.`school_id`, f.`applicable_type`, f.`applicable_sub_type`;

-- 5.3 异常检查：本次生成的流程存在但无步骤（应为 0 行）
SELECT f.`school_id`, f.`applicable_type`, f.`applicable_sub_type`, f.`flow_name`
FROM `approval_flows` f
JOIN tmp_target t
  ON t.`applicable_type` = f.`applicable_type`
 AND t.`applicable_sub_type` <=> f.`applicable_sub_type`
WHERE f.`deleted_at` IS NULL
  AND f.`flow_name` = t.`flow_name`
  AND NOT EXISTS (
      SELECT 1 FROM `approval_flow_steps` x
      WHERE x.`flow_id` = f.`id` AND x.`deleted_at` IS NULL
  );

DROP TEMPORARY TABLE IF EXISTS tmp_src_flow;
DROP TEMPORARY TABLE IF EXISTS tmp_target;
DROP TEMPORARY TABLE IF EXISTS tmp_next_version;

-- ------------------------------------------------------------
-- 回滚（软删除本次通过克隆生成的流程及其步骤、映射）
-- 说明：通过 copied_from 指向学科竞赛流程识别本次生成的数据，
--       不触碰学科竞赛原流程；如需物理删除，自行改 UPDATE 为 DELETE。
-- ------------------------------------------------------------
-- UPDATE `approval_flow_mappings` SET `deleted_at` = NOW()
-- WHERE `deleted_at` IS NULL
--   AND `flow_id` IN (
--       SELECT `id` FROM (
--           SELECT `id` FROM `approval_flows`
--           WHERE `deleted_at` IS NULL AND `version` = 1 AND `copied_from` IS NOT NULL
--       ) x
--   );
-- UPDATE `approval_flow_steps` SET `deleted_at` = NOW()
-- WHERE `deleted_at` IS NULL
--   AND `flow_id` IN (
--       SELECT `id` FROM (
--           SELECT `id` FROM `approval_flows`
--           WHERE `deleted_at` IS NULL AND `version` = 1 AND `copied_from` IS NOT NULL
--       ) x
--   );
-- UPDATE `approval_flows` SET `deleted_at` = NOW(), `status` = 0
-- WHERE `deleted_at` IS NULL AND `version` = 1 AND `copied_from` IS NOT NULL;
