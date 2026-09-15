-- ============================================================
-- 种子数据：2 条完整管理员数据
--
-- 为系统初始化 2 名管理员（用户 id=6/7），补齐管理端（/admin/*）所需的
-- 全部关联数据：用户 + 管理员角色 + 权限（菜单/API）+ 角色-权限关联
--              + 用户-角色关联 + 校级角色范围 + 联系信息。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql        （schools/colleges/majors/classes/users 1~5）
--   2. seed_roles_permissions.sql（roles 1 / user_roles 1~5；permissions 由 Flyway V35 维护）
--   另：schema 与 permissions 字典由 Flyway 迁移负责（V1~V35），本脚本只补演示数据。
--
-- 数据口径（与《管理端接口文档》V5.6 权限控制对齐）：
--   - 角色 code='admin'，level=0（RoleLevelEnum.SYSTEM），role_type=4（系统管理类）
--   - 关键权限码：user:view / user:manage / system:role:manage / org:manage
--                semester:manage / semester:import / dictionary:manage / approval:flow:manage
--                indicator:manage / score:recalculate / grade:import
--                export:research / export:manage / export:template:manage
--                archive:view / archive:export / statistics:view
--                form:template:manage / log:view / audit:revoke
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
-- 3. 管理员权限（permissions）
--    菜单（type=1）为父节点，API（type=3）挂载其下。
--    权限字典已迁移至 Flyway（V35），此处保留占位说明，不再插入行。
--    （与《管理端接口文档》V5.6 关键权限码表一致，代码层为唯一校验口径）
-- ============================================================

-- 3.1~3.5 权限字典（permissions）
--    已由 Flyway 迁移统一维护：
--      db/migration/V35__ensure_role_permissions_and_scopes.sql
--    本脚本不再插入 permissions 行。原因：新库中迁移先于本脚本执行，迁移会以自增 id
--    建好全部权限码，若此处再按 id 8~23/37/38~43 插入会撞 uk_permissions_code 唯一键。
--    原 3.5 节（代码层已校验但种子缺失的 6 个权限码 user:view/archive:view/statistics:view/
--    archive:export/form:template:manage/semester:import）同样由 V35 建好。

-- ============================================================
-- 4. 角色-权限关联（role_permissions）
--    超级管理员（role_id=2）授予管理端全部 23 个权限（含菜单）。
--    按权限码关联，不写死 id；INSERT IGNORE 保证可重复执行。
--    注意：审批委托 delegate:manage 为教师专属（见 seed_teachers.sql），不授予管理员；
--    管理员在「审批流程配置」模块指定各审批节点的审核员。
-- ============================================================
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`deleted_at` IS NULL AND p.`code` IN (
    -- 菜单
    'system:manage', 'data:manage', 'log:audit',
    -- 系统管理模块
    'user:manage', 'system:role:manage', 'org:manage', 'semester:manage',
    'dictionary:manage', 'approval:flow:manage',
    -- 数据管理模块
    'indicator:manage', 'score:recalculate', 'grade:import',
    'export:research', 'export:manage', 'export:template:manage',
    -- 日志审计模块
    'log:view', 'audit:revoke',
    -- 代码层校验的补充权限码
    'user:view', 'archive:view', 'statistics:view', 'archive:export',
    'form:template:manage', 'semester:import'
)
WHERE r.`deleted_at` IS NULL AND r.`code` = 'admin';

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
