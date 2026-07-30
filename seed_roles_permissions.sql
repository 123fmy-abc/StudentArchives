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
-- 2. 学生权限（permissions，type=3 表示 API 接口权限）
-- ============================================================
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(1, '查看个人档案', 'student:archive:view',   3, NULL, 1, 1),
(2, '创建个人档案', 'student:archive:create', 3, NULL, 2, 1),
(3, '编辑个人档案', 'student:archive:edit',   3, NULL, 3, 1),
(4, '删除个人档案', 'student:archive:delete', 3, NULL, 4, 1),
(5, '查看个人信息', 'student:profile:view',   3, NULL, 5, 1),
(6, '编辑个人信息', 'student:profile:edit',   3, NULL, 6, 1),
(7, '修改密码',     'student:auth:password',  3, NULL, 7, 1);

-- ============================================================
-- 3. 角色-权限关联（role_permissions）
-- ============================================================
INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`) VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 1, 5),
(6, 1, 6),
(7, 1, 7);

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
