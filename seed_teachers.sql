-- ============================================================
-- 种子数据：3 条完整教师数据（含辅导员角色）
--
-- 为系统初始化 3 名教师（用户 id=8~10），补齐教师端所需的
-- 全部关联数据：用户 + 教师档案 + 教师角色 + 辅导员角色 + 权限（API，id=24~36）
--              + 角色-权限关联 + 用户-角色关联 + 学院/班级范围 + 联系信息。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql        （schools/colleges/majors/classes/users 1~5）
--   2. seed_roles_permissions.sql（roles 1 / permissions 1~7 / user_roles 1~5）
--   3. seed_admins.sql          （roles 2 / permissions 8~23、37 / users 6~7）
--
-- 数据口径（与《RoleLevelEnum / RoleTypeEnum / 教师端接口文档》V5.6 对齐）：
--   - 教师角色 code='teacher'，level=2（RoleLevelEnum.TEACHER 教师）
--   - 辅导员角色 code='counselor'，level=3（RoleLevelEnum.COUNSELOR 辅导员）
--   - 辅导员也是教师：仅讲师孙志强（用户 10）兼任辅导员，绑定 teacher + counselor 双角色
--     （教授王建国 / 副教授刘秀英为纯教师角色，用于验证权限边界）
--   - 两个角色均 role_type=1（教学类）、is_auditor=1（可作为审批节点/被委托）
--   - teacher 范围 scope_types='[2,3,4]'（学院/专业/班级）
--   - counselor 范围 scope_types='[4]'（班级，与《管理端接口文档》5.3"辅导员审核"节点一致）
--   - 两个角色共享同一套权限码（permissions 24~36 + 复用 score:recalculate id=18），
--     覆盖《教师端接口文档》§6 + 附录A
--   - audit:revoke（撤销审核）/ export:research（研究数据导出）仅授予管理员，普通教师不授予
--   - 教师职称 title 直接存展示值（教授/副教授/讲师）
-- ============================================================

-- ============================================================
-- 1. 教师用户（users）
--    工号 T00001~T00003，密码统一 "123456"（Bcrypt，与学生/管理员一致）
--    沿用学生 id=1~5、管理员 id=6~7，教师从 id=8 开始
-- ============================================================
INSERT INTO `users` (`id`, `school_id`, `user_no`, `name`, `gender`, `birth_date`, `password`, `status`) VALUES
(8,  1, 'T00001', '王建国', 1, '1978-04-12', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(9,  1, 'T00002', '刘秀英', 2, '1985-09-03', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(10, 1, 'T00003', '孙志强', 1, '1992-02-27', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1);

-- ============================================================
-- 2. 教师档案（teacher_profiles，id=1~3）
--    college_id=1 → 计算机科学与技术学院
--    title 职称直接存展示值（与教师端 GET /teacher/profile 返回口径一致）
-- ============================================================
INSERT INTO `teacher_profiles` (`id`, `user_id`, `college_id`, `title`) VALUES
(1, 8,  1, '教授'),
(2, 9,  1, '副教授'),
(3, 10, 1, '讲师');

-- ============================================================
-- 3. 教师角色 + 辅导员角色（roles，id=3~4）
--    teacher    level=2（RoleLevelEnum.TEACHER）   scope_types='[2,3,4]'
--    counselor  level=3（RoleLevelEnum.COUNSELOR） scope_types='[4]'
--    is_auditor=1 → 均可作为审批节点（审核档案/奖项/职业规划），且能被委托审批
-- ============================================================
INSERT INTO `roles` (`id`, `name`, `code`, `description`, `level`, `role_type`, `is_system`, `is_auditor`, `scope_types`, `max_scope_count`, `status`) VALUES
(3, '教师', 'teacher', '教师角色，可审核授权范围内学生申报、查看学生档案、导出数据、管理审批委托', 2, 1, 1, 1, '[2,3,4]', 0, 1),
(4, '辅导员', 'counselor', '辅导员角色，负责班级学生管理与初审（辅导员也是教师，同一用户可同时绑定两角色）', 3, 1, 1, 1, '[4]', 0, 1);

-- ============================================================
-- 4. 教师权限（permissions，id=24~36，type=3 表示 API 接口权限）
--    id=24~28：个人中心（与教师端 /teacher/profile、/auth/password 对应）
--    id=29~36：核心业务权限码（与《教师端接口文档》§6 关键权限码 + 附录A 对照表对齐）
--       score:recalculate 复用管理员种子已建权限（id=18），此处不重复插入
--       audit:revoke / export:research 不授予普通教师（仅管理员）
-- ============================================================
INSERT INTO `permissions` (`id`, `name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
(24, '查看个人档案',   'teacher:archive:view',  3, NULL, 1, 1),
(25, '编辑个人档案',   'teacher:archive:edit',  3, NULL, 2, 1),
(26, '查看个人信息',   'teacher:profile:view',  3, NULL, 3, 1),
(27, '编辑个人信息',   'teacher:profile:edit',  3, NULL, 4, 1),
(28, '修改密码',       'teacher:auth:password', 3, NULL, 5, 1),
(29, '教师首页',       'dashboard:view',        3, NULL, 6, 1),
(30, '待审核列表',     'audit:pending',         3, NULL, 7, 1),
(31, '审核通过',       'audit:approve',         3, NULL, 8, 1),
(32, '批量审核',       'audit:batch',           3, NULL, 9, 1),
(33, '查看学生',       'student:view',          3, NULL, 10, 1),
(34, 'AI评价生成',     'ai:invoke',             3, NULL, 11, 1),
(35, '数据导出',       'export:execute',        3, NULL, 12, 1),
(36, '审批委托管理',   'delegate:manage',       3, NULL, 13, 1);

-- ============================================================
-- 5. 角色-权限关联（role_permissions，id=24~51）
--    教师角色（role_id=3）与辅导员角色（role_id=4）共享同一套
--    权限码（permissions 24~36 + 共享 score:recalculate id=18）
--    辅导员无需重复定义权限
-- ============================================================
INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`) VALUES
(24, 3, 24),
(25, 3, 25),
(26, 3, 26),
(27, 3, 27),
(28, 3, 28),
(29, 3, 29),
(30, 3, 30),
(31, 3, 31),
(32, 3, 32),
(33, 3, 33),
(34, 3, 34),
(35, 3, 18),
(36, 3, 35),
(37, 3, 36),
(38, 4, 24),
(39, 4, 25),
(40, 4, 26),
(41, 4, 27),
(42, 4, 28),
(43, 4, 29),
(44, 4, 30),
(45, 4, 31),
(46, 4, 32),
(47, 4, 33),
(48, 4, 34),
(49, 4, 18),
(50, 4, 35),
(51, 4, 36);

-- ============================================================
-- 6. 用户-角色关联（user_roles，id=8~11）
--    三名教师均绑定教师角色（role_id=3）
--    仅讲师孙志强（user_id=10）兼任辅导员（role_id=4），构成双角色
--    （与《教师端接口文档》/auth/me 示例 roles: ["teacher","counselor"] 一致）
-- ============================================================
INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(8,  8,  3),
(9,  9,  3),
(10, 10, 3),
(11, 10, 4);

-- ============================================================
-- 7. 角色组织范围绑定（role_scopes，id=3~6）
--    id=3~5：teacher 角色（role_id=3）→ 学院范围 scope_type=2，scope_id=1（3 名教师）
--    id=6：  counselor 角色（role_id=4）→ 班级范围 scope_type=4，scope_id=1（仅用户 10）
--    （同一用户两个角色分别绑定各自范围，唯一键不同互不冲突）
--    is_primary=1 → 主职；valid_from=2024-09-01 起永久有效
-- ============================================================
INSERT INTO `role_scopes` (`id`, `school_id`, `user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`, `is_primary`, `appoint_by`, `appoint_reason`, `valid_from`, `valid_until`, `status`) VALUES
(3, 1, 8,  3, 2, 1, NULL, 1, 6, '系统初始化：教师学院范围',   '2024-09-01', NULL, 1),
(4, 1, 9,  3, 2, 1, NULL, 1, 6, '系统初始化：教师学院范围',   '2024-09-01', NULL, 1),
(5, 1, 10, 3, 2, 1, NULL, 1, 6, '系统初始化：教师学院范围',   '2024-09-01', NULL, 1),
(6, 1, 10, 4, 4, 1, NULL, 1, 6, '系统初始化：辅导员班级范围', '2024-09-01', NULL, 1);

-- ============================================================
-- 8. 教师联系信息（user_contact_infos，id=8~10）
-- ============================================================
INSERT INTO `user_contact_infos` (`id`, `user_id`, `phone`, `email`, `address`) VALUES
(8,  8,  '13700001008', 'wangjianguo@hust.edu.cn', '华中科技大学西区教师公寓8栋101室'),
(9,  9,  '13700001009', 'liuxiuying@hust.edu.cn', '华中科技大学西区教师公寓8栋102室'),
(10, 10, '13700001010', 'sunzhiqiang@hust.edu.cn', '华中科技大学西区教师公寓8栋103室');
