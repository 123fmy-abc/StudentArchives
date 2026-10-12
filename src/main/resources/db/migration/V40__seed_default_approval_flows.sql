-- ============================================================
-- V40：为每个学校补齐默认审批流程（开箱即用，避免申报无人审核）
--
-- 背景（2026-10-12 审核流程配置复核）：
--   approval_flows / approval_flow_steps / approval_flow_mappings 此前无任何默认数据，
--   提交后解析不到流程时仅「静默跳过」，导致任何申报都不会生成待办、无人审核。
--   本迁移为每所学校 × 三种适用类型各建一条「默认审批流程」（单步：校级管理员审核），
--   使 resolveFlow 的 is_default=1 兜底路径始终可命中；步骤角色指向 code='admin'，
--   范围 scope_type=1（学校），配合 V35 已为管理员补齐的校级授权，可稳定解析到审批人。
--
-- 依赖与边界：
--   - 角色表 roles 需已存在 code='admin' 的角色（seed_admins.sql / V35 维护）。
--     若某库没有 admin 角色，本迁移 graceful 跳过（INSERT..SELECT 产出 0 行），不报错。
--   - 仅「补齐」，不覆盖管理员已配置的流程/步骤（NOT EXISTS 守卫，且避开
--     uk_af_version 唯一键冲突）。
-- ============================================================

-- 1. 默认流程（每学校 × 三类型，applicable_sub_type=NULL，is_default=1，version=1）
INSERT INTO `approval_flows`
    (`school_id`, `flow_name`, `applicable_type`, `applicable_sub_type`, `version`,
     `is_default`, `status`, `created_by`)
SELECT s.`id`, '默认审批流程', t.`applicable_type`, NULL, 1, 1, 1,
       IFNULL((SELECT MIN(u.`id`) FROM `users` u WHERE u.`deleted_at` IS NULL), 0)
FROM `schools` s
CROSS JOIN (
              SELECT 'Archive'         AS `applicable_type`
    UNION ALL SELECT 'AwardApplication'
    UNION ALL SELECT 'CareerPlan'
) t
WHERE s.`deleted_at` IS NULL
  AND EXISTS (SELECT 1 FROM `roles` r WHERE r.`code` = 'admin' AND r.`deleted_at` IS NULL)
  AND NOT EXISTS (
        SELECT 1 FROM `approval_flows` f
        WHERE f.`school_id` = s.`id`
          AND f.`applicable_type` = t.`applicable_type`
          AND f.`deleted_at` IS NULL
          AND (f.`is_default` = 1 OR (f.`applicable_sub_type` IS NULL AND f.`version` = 1))
  );

-- 2. 默认步骤（单步：校级管理员审核，scope_type=1 学校范围）
INSERT INTO `approval_flow_steps`
    (`flow_id`, `step_no`, `step_name`, `role_id`, `scope_type`, `scope_rule`,
     `auto_assign`, `allow_delegate`, `allow_skip`, `allow_designate_next`,
     `timeout_hours`, `reject_action`, `reject_to_step`, `sort`)
SELECT f.`id`, 1, '管理员审核',
       (SELECT r.`id` FROM `roles` r WHERE r.`code` = 'admin' AND r.`deleted_at` IS NULL
        ORDER BY r.`id` LIMIT 1),
       1, 'school', 1, 0, 0, 0, 48, 'end', NULL, 0
FROM `approval_flows` f
WHERE f.`deleted_at` IS NULL
  AND f.`is_default` = 1
  AND f.`applicable_sub_type` IS NULL
  AND f.`flow_name` = '默认审批流程'
  AND NOT EXISTS (
        SELECT 1 FROM `approval_flow_steps` s
        WHERE s.`flow_id` = f.`id` AND s.`deleted_at` IS NULL
  );
