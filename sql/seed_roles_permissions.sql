-- ============================================================
-- 种子数据：学生角色与权限
-- 包含角色 + 权限 + 角色-权限关联 + 用户-角色关联
-- 前提：已执行 seed_students.sql，users 表存在 ID 1~5 的学生
-- ============================================================

-- ============================================================
-- 1. 学生角色（roles）
-- level=1 → RoleLevelEnum.STUDENT
-- role_type=1 → RoleTypeEnum.TEACHING（教学类）
-- is_system=1 → 系统内置（不可删除）
-- scope_types='[4]' → 班级范围
-- ============================================================
INSERT INTO `roles` (`id`, `name`, `code`, `description`, `level`, `role_type`, `is_system`, `is_auditor`, `scope_types`, `max_scope_count`, `status`) VALUES
(1, '学生', 'student', '学生角色，可管理个人档案和查看个人信息', 1, 1, 1, 0, '[4]', 0, 1);

-- ============================================================
-- 2. 学生权限（permissions）
--    权限字典已由 Flyway 迁移统一维护：
--      db/migration/V35__ensure_role_permissions_and_scopes.sql
--    本脚本不再插入 permissions 行。原因：新库中迁移先于本脚本执行，迁移会以自增 id
--    建好全部权限码，若此处再按 id 1~7 插入会撞 uk_permissions_code 唯一键。
-- ============================================================

-- ============================================================
-- 3. 角色-权限关联（role_permissions）
--    按权限码关联，不写死 id；INSERT IGNORE 保证可重复执行。
-- ============================================================
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`deleted_at` IS NULL AND p.`code` IN (
    'student:archive:view', 'student:archive:create', 'student:archive:edit',
    'student:archive:delete', 'student:profile:view', 'student:profile:edit',
    'student:auth:password'
)
WHERE r.`deleted_at` IS NULL AND r.`code` = 'student';

-- ============================================================
-- 4. 用户-角色关联（user_roles）
-- 关联 seed_students.sql 中的 5 条学生用户
-- ============================================================
INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 3, 1),
(4, 4, 1),
(5, 5, 1);
