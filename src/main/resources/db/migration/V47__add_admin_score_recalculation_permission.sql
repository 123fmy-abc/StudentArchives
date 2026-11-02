-- V47: 拆分管理端评分重算权限（AUTHZ-01 / BSR-08）
--
-- 背景
--   教师/辅导员默认持有 score:recalculate（教师端 11.4 复用管理端引擎），而管理端
--   POST /admin/scores/recalculate 与 GET /admin/scores/recalculation-tasks/{taskId}
--   此前也校验同一权限码，导致教师令牌可触发全校/全量重算并读取任意任务详情。
--
-- 方案
--   新增管理端专属权限码 score:recalculate:admin，仅授予 admin 角色及此前已持有
--   score:recalculate 的非教师角色；教师侧保留 score:recalculate，只用于 /teacher/scores/*。
--   admin 角色在 AdminAuthService 中直接放行，无需依赖本迁移授权。
--
-- 幂等：permissions 依赖 uk_permissions_code，role_permissions 依赖
--       uk_role_permissions，均使用 INSERT IGNORE，可在任意库重复执行。

INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`)
SELECT '管理端评分重算', 'score:recalculate:admin', 3, par.`id`, 7, 1
FROM `permissions` par
WHERE par.`code` = 'data:manage' AND par.`deleted_at` IS NULL;

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'score:recalculate:admin' AND p.`deleted_at` IS NULL
WHERE r.`deleted_at` IS NULL
  AND (
    r.`code` = 'admin'
    OR (
      r.`code` NOT IN ('teacher', 'counselor')
      AND EXISTS (
        SELECT 1
        FROM `role_permissions` rp
        JOIN `permissions` old ON old.`id` = rp.`permission_id`
        WHERE rp.`role_id` = r.`id`
          AND rp.`deleted_at` IS NULL
          AND old.`code` = 'score:recalculate'
          AND old.`deleted_at` IS NULL
      )
    )
  );
