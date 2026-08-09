-- ============================================================
-- 种子数据：2 条完整管理员数据
--
-- 为系统初始化 2 名管理员（用户 id=6/7），补齐管理端（/admin/*）所需的
-- 全部关联数据：用户 + 管理员角色 + 权限（菜单/API）+ 角色-权限关联
--              + 用户-角色关联 + 校级角色范围 + 联系信息。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql        （schools/colleges/majors/classes/users 1~5）
--   2. seed_roles_permissions.sql（roles 1 / permissions 1~7 / user_roles 1~5）
--
-- 数据口径（与《管理端接口文档》V5.6 权限控制对齐）：
--   - 角色 code='admin'，level=0（RoleLevelEnum.SYSTEM），role_type=4（系统管理类）
--   - 关键权限码：indicator:manage / score:recalculate / log:view
--                export:research / export:manage / export:template:manage
--                / audit:revoke / grade:import
--   - 管理员通过 role_scopes 绑定学校范围（scope_type=1），实现全校数据权限
-- ============================================================

-- ============================================================
-- 1. 管理员用户（users）
--    工号 A00001/A00002，密码统一 "123456"（Bcrypt，与学生一致）
--    沿用 seed_students.sql 的学生 id=1~5，管理员从 id=6 开始
-- ============================================================
INSERT INTO `users` (`id`, `school_id`, `user_no`, `name`, `gender`, `birth_date`, `password`, `status`) VALUES
(6, 1, 'A00001', '管理员A', 1, '1985-05-20', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(7, 1, 'A00002', '管理员B', 2, '1990-11-08', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1);

-- ============================================================
-- 2. 超级管理员角色（roles，id=2）
--    level=0      → RoleLevelEnum.SYSTEM（系统）
--    role_type=4  → RoleTypeEnum.SYSTEM_MANAGEMENT（系统管理类）
--    is_system=1  → 系统内置（不可删除）
--    is_auditor=1 → 可作为审批节点（支持撤销审核等审计操作）
--    scope_types='[1,2,3,4]' → 学校/学院/专业/班级范围
-- ============================================================
INSERT INTO `roles` (`id`, `name`, `code`, `description`, `level`, `role_type`, `is_system`, `is_auditor`, `scope_types`, `max_scope_count`, `status`) VALUES
(2, '超级管理员', 'admin', '系统管理员，拥有系统全部管理权限', 0, 4, 1, 1, '[1,2,3,4]', 0, 1);

-- ============================================================
-- 3. 管理员权限（permissions，id=8~23）
--    菜单（type=1）为父节点，API（type=3）挂载其下
--    id=8~10 为菜单；id=11~23 为管理端关键权限码
--    （与《管理端接口文档》V5.6 关键权限码表一致）
-- ============================================================

-- 3.1 菜单权限（type=1）
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(8,  '系统管理', 'system:manage', 1, NULL, 1, 1),
(9,  '数据管理', 'data:manage',   1, NULL, 2, 1),
(10, '日志审计', 'log:audit',     1, NULL, 3, 1);

-- 3.2 API 权限（type=3）— 系统管理模块
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(11, '用户管理',         'system:user:manage',   3, 8,  1, 1),
(12, '角色权限管理',     'system:role:manage',   3, 8,  2, 1),
(13, '组织架构管理',     'org:manage',           3, 8,  3, 1),
(14, '学期管理',         'semester:manage',      3, 8,  4, 1),
(15, '字典管理',         'dictionary:manage',    3, 8,  5, 1),
(16, '审批流程配置',     'approval:flow:manage', 3, 8,  6, 1);

-- 3.3 API 权限（type=3）— 数据管理模块
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(17, '指标配置管理', 'indicator:manage',   3, 9, 1, 1),
(18, '触发评分重算', 'score:recalculate',  3, 9, 2, 1),
(19, '成绩导入',     'grade:import',       3, 9, 3, 1),
(20, '研究数据导出', 'export:research',    3, 9, 4, 1),
(21, '管理端数据导出', 'export:manage',    3, 9, 5, 1),
(37, '导出模板管理', 'export:template:manage', 3, 9, 6, 1);

-- 3.4 API 权限（type=3）— 日志审计模块
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(22, '查看操作日志', 'log:view',      3, 10, 1, 1),
(23, '撤销审核',     'audit:revoke',  3, 10, 2, 1);

-- ============================================================
-- 4. 角色-权限关联（role_permissions，id=8~23 + 52）
--    超级管理员（role_id=2）授予上述全部 17 个权限（含菜单）
--    id=52：导出模板管理 export:template:manage（permission id=37）
--    （permission/role_permissions 的 24~51 已由 seed_teachers.sql 占用，故新权限从 37/52 起）
-- ============================================================
INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`) VALUES
(8,  2, 8),
(9,  2, 9),
(10, 2, 10),
(11, 2, 11),
(12, 2, 12),
(13, 2, 13),
(14, 2, 14),
(15, 2, 15),
(16, 2, 16),
(17, 2, 17),
(18, 2, 18),
(19, 2, 19),
(20, 2, 20),
(21, 2, 21),
(22, 2, 22),
(23, 2, 23),
(52, 2, 37);

-- ============================================================
-- 5. 用户-角色关联（user_roles，id=6~7）
--    两名管理员均绑定超级管理员角色
-- ============================================================
INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(6, 6, 2),
(7, 7, 2);

-- ============================================================
-- 6. 角色组织范围绑定（role_scopes，id=1~2）
--    scope_type=1 → 学校范围，scope_id=1 → 华中科技大学
--    is_primary=1 → 主职；valid_from=2024-09-01 起永久有效
-- ============================================================
INSERT INTO `role_scopes` (`id`, `school_id`, `user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`, `is_primary`, `appoint_by`, `appoint_reason`, `valid_from`, `valid_until`, `status`) VALUES
(1, 1, 6, 2, 1, 1, NULL, 1, NULL, '系统初始化：校级管理员', '2024-09-01', NULL, 1),
(2, 1, 7, 2, 1, 1, NULL, 1, NULL, '系统初始化：校级管理员', '2024-09-01', NULL, 1);

-- ============================================================
-- 7. 管理员联系信息（user_contact_infos，id=6~7）
-- ============================================================
INSERT INTO `user_contact_infos` (`id`, `user_id`, `phone`, `email`, `address`) VALUES
(6, 6, '13900001006', 'admin1@hust.edu.cn', '华中科技大学行政楼101室'),
(7, 7, '13900001007', 'admin2@hust.edu.cn', '华中科技大学行政楼102室');
