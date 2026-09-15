-- ============================================================
-- V35：收敛角色权限与数据范围（对应《后端问题清单》P0-1 / P0-2 / P0-3）
--
-- 背景
--   permissions / role_permissions / role_scopes 这三张表的种子数据此前只存在于仓库根目录的
--   手工脚本（seed_roles_permissions.sql / seed_admins.sql / seed_teachers.sql），
--   Flyway 迁移从不插入这些行。于是任何没跑过手工脚本的库都会出现 /auth/me 的
--   permissions、scopes 双空，表现为：
--     - 管理端菜单 19 → 1 项、教师端菜单 5 → 1 项（P0-1）
--     - 教师端日志必然 20005（log:view 只授给了 admin，P0-2）
--     - 教师端首页「档案数据概览」整块 403（role_scopes 为空，P0-3）
--
-- 设计原则
--   只按 `code` 认数据，不硬编码任何 id。三张表的唯一键天然支持幂等：
--     permissions        UNIQUE (code, is_deleted_null)
--     role_permissions   UNIQUE (role_id, permission_id, is_deleted_null)
--     role_scopes        UNIQUE (user_id, role_id, scope_type, scope_id, semester_id, is_deleted_null)
--   因此本迁移可在任意库（空库 / 开发库 / 线上库）重复执行并收敛到同一结果，
--   且不依赖种子脚本里那套 id 约定（线上真实数据的 id 与种子并不一致）。
--
-- 边界
--   本迁移只做「补齐」，不做「回收」——不删除任何既有授权。
--   权限字典自此由迁移维护，三份种子脚本不再插入 permissions 行（见各脚本头部说明）。
--   辅导员（counselor）的班级范围没有可派生的数据来源（classes 表无辅导员列、
--   无辅导员↔班级关联表），故不在本迁移内自动生成，由管理员在用户管理页配置。
-- ============================================================


-- ------------------------------------------------------------
-- 1. 权限字典 —— 菜单与无父节点的权限码
--    （INSERT IGNORE 依赖 uk_permissions_code 去重，已存在则跳过）
-- ------------------------------------------------------------
INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`) VALUES
-- 1.1 管理端菜单（type=1，作为下方 API 权限的父节点）
('系统管理', 'system:manage', 1, NULL, 1, 1),
('数据管理', 'data:manage',   1, NULL, 2, 1),
('日志审计', 'log:audit',     1, NULL, 3, 1),

-- 1.2 学生端（type=3）
('查看个人档案', 'student:archive:view',   3, NULL, 1, 1),
('创建个人档案', 'student:archive:create', 3, NULL, 2, 1),
('编辑个人档案', 'student:archive:edit',   3, NULL, 3, 1),
('删除个人档案', 'student:archive:delete', 3, NULL, 4, 1),
('查看个人信息', 'student:profile:view',   3, NULL, 5, 1),
('编辑个人信息', 'student:profile:edit',   3, NULL, 6, 1),
('修改密码',     'student:auth:password',  3, NULL, 7, 1),

-- 1.3 教师端 / 辅导员（type=3）
('查看个人档案', 'teacher:archive:view',   3, NULL, 1, 1),
('编辑个人档案', 'teacher:archive:edit',   3, NULL, 2, 1),
('查看个人信息', 'teacher:profile:view',   3, NULL, 3, 1),
('编辑个人信息', 'teacher:profile:edit',   3, NULL, 4, 1),
('修改密码',     'teacher:auth:password',  3, NULL, 5, 1),
('教师首页',     'dashboard:view',         3, NULL, 6, 1),
('待审核列表',   'audit:pending',          3, NULL, 7, 1),
('审核通过',     'audit:approve',          3, NULL, 8, 1),
('批量审核',     'audit:batch',            3, NULL, 9, 1),
('查看学生',     'student:view',           3, NULL, 10, 1),
('AI评价生成',   'ai:invoke',             3, NULL, 11, 1),
('数据导出',     'export:execute',         3, NULL, 12, 1),
('审批委托管理', 'delegate:manage',        3, NULL, 13, 1),

-- 1.4 管理端补充权限码（代码层已校验，此前只存在于手工脚本 §3.5）
('用户查看',     'user:view',             3, NULL, 7, 1),
('档案查看',     'archive:view',          3, NULL, 7, 1),
('统计查看',     'statistics:view',       3, NULL, 8, 1),
('档案导出',     'archive:export',        3, NULL, 9, 1),
('表单模板管理', 'form:template:manage',  3, NULL, 8, 1),
('学期导入',     'semester:import',       3, NULL, 9, 1);


-- ------------------------------------------------------------
-- 2. 权限字典 —— 挂菜单节点下的管理端 API 权限
--    parent_id 由父级 code 反查得到，不写死 id。
--    （MySQL 允许 INSERT ... SELECT 的 FROM 子句出现目标表本身）
-- ------------------------------------------------------------
INSERT IGNORE INTO `permissions` (`name`, `code`, `type`, `parent_id`, `sort`, `status`)
SELECT v.`name`, v.`code`, 3, par.`id`, v.`sort`, 1
FROM (
              SELECT '用户管理'     AS `name`, 'user:manage'           AS `code`, 'system:manage' AS `parent_code`, 1 AS `sort`
    UNION ALL SELECT '角色权限管理',       'system:role:manage',                  'system:manage',          2
    UNION ALL SELECT '组织架构管理',       'org:manage',                          'system:manage',          3
    UNION ALL SELECT '学期管理',           'semester:manage',                     'system:manage',          4
    UNION ALL SELECT '字典管理',           'dictionary:manage',                   'system:manage',          5
    UNION ALL SELECT '审批流程配置',       'approval:flow:manage',                'system:manage',          6
    UNION ALL SELECT '指标配置管理',       'indicator:manage',                    'data:manage',            1
    UNION ALL SELECT '触发评分重算',       'score:recalculate',                   'data:manage',            2
    UNION ALL SELECT '成绩导入',           'grade:import',                        'data:manage',            3
    UNION ALL SELECT '研究数据导出',       'export:research',                     'data:manage',            4
    UNION ALL SELECT '管理端数据导出',     'export:manage',                       'data:manage',            5
    UNION ALL SELECT '导出模板管理',       'export:template:manage',              'data:manage',            6
    UNION ALL SELECT '查看操作日志',       'log:view',                            'log:audit',              1
    UNION ALL SELECT '撤销审核',           'audit:revoke',                        'log:audit',              2
) v
JOIN `permissions` par ON par.`code` = v.`parent_code` AND par.`deleted_at` IS NULL;


-- ------------------------------------------------------------
-- 3. 角色授权（role_permissions）
--    admin    ：管理端全部权限（含菜单），不含 delegate:manage
--    student  ：学生端 7 个
--    teacher  ：教师端 13 个 + score:recalculate + log:view（P0-2）
--    counselor：与 teacher 同一套
--
--    说明：审批委托（delegate:manage）为教师专属，由管理员在「审批流程配置」
--          模块指定各审批节点的审核员；不授予 admin，也不回收教师侧既有授权。
-- ------------------------------------------------------------

-- 3.1 admin：管理端全部权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`deleted_at` IS NULL AND p.`code` IN (
    'system:manage', 'data:manage', 'log:audit',
    'user:manage', 'system:role:manage', 'org:manage', 'semester:manage',
    'dictionary:manage', 'approval:flow:manage',
    'indicator:manage', 'score:recalculate', 'grade:import',
    'export:research', 'export:manage', 'export:template:manage',
    'log:view', 'audit:revoke',
    'user:view', 'archive:view', 'statistics:view', 'archive:export',
    'form:template:manage', 'semester:import'
)
WHERE r.`deleted_at` IS NULL AND r.`code` = 'admin';

-- 3.2 student：学生端权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`deleted_at` IS NULL AND p.`code` IN (
    'student:archive:view', 'student:archive:create', 'student:archive:edit',
    'student:archive:delete', 'student:profile:view', 'student:profile:edit',
    'student:auth:password'
)
WHERE r.`deleted_at` IS NULL AND r.`code` = 'student';

-- 3.3 teacher / counselor：教师端权限 + score:recalculate + log:view
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `roles` r
JOIN `permissions` p ON p.`deleted_at` IS NULL AND p.`code` IN (
    'teacher:archive:view', 'teacher:archive:edit',
    'teacher:profile:view', 'teacher:profile:edit', 'teacher:auth:password',
    'dashboard:view', 'audit:pending', 'audit:approve', 'audit:batch',
    'student:view', 'ai:invoke', 'export:execute', 'delegate:manage',
    -- 报告 P0-2：教师端日志接口校验 log:view，但该码此前只授给 admin
    'log:view',
    -- 教师端评分重算复用管理端权限码
    'score:recalculate'
)
WHERE r.`deleted_at` IS NULL AND r.`code` IN ('teacher', 'counselor');


-- ------------------------------------------------------------
-- 4. 数据范围（role_scopes）—— 报告 P0-3
--    教师端统计/档案范围筛选直接读 role_scopes，为空即 403。
--    admin  → 校级（scope_type=1），取用户自身 school_id
--    teacher→ 学院级（scope_type=2），取教师档案 teacher_profiles.college_id
--    仅对「已有角色、但一条生效范围都没有」的用户补一条主职范围，不覆盖人工配置。
-- ------------------------------------------------------------

-- 4.1 管理员：校级范围
INSERT IGNORE INTO `role_scopes`
    (`school_id`, `user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`,
     `is_primary`, `appoint_by`, `appoint_reason`, `valid_from`, `valid_until`, `status`)
SELECT u.`school_id`, u.`id`, r.`id`, 1, u.`school_id`, NULL,
       1, NULL, '数据修复：V35 自动补全校级范围', CURDATE(), NULL, 1
FROM `users` u
JOIN `user_roles` ur ON ur.`user_id` = u.`id` AND ur.`deleted_at` IS NULL
JOIN `roles` r ON r.`id` = ur.`role_id` AND r.`deleted_at` IS NULL AND r.`code` = 'admin'
WHERE u.`deleted_at` IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM (
          SELECT `user_id`, `role_id` FROM `role_scopes` WHERE `deleted_at` IS NULL AND `status` = 1
      ) x WHERE x.`user_id` = u.`id` AND x.`role_id` = r.`id`
  );

-- 4.2 教师：学院范围（来源：教师档案所属学院）
INSERT IGNORE INTO `role_scopes`
    (`school_id`, `user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`,
     `is_primary`, `appoint_by`, `appoint_reason`, `valid_from`, `valid_until`, `status`)
SELECT u.`school_id`, u.`id`, r.`id`, 2, tp.`college_id`, NULL,
       1, NULL, '数据修复：V35 按教师档案学院补全范围', CURDATE(), NULL, 1
FROM `users` u
JOIN `user_roles` ur ON ur.`user_id` = u.`id` AND ur.`deleted_at` IS NULL
JOIN `roles` r ON r.`id` = ur.`role_id` AND r.`deleted_at` IS NULL AND r.`code` = 'teacher'
JOIN `teacher_profiles` tp ON tp.`user_id` = u.`id` AND tp.`deleted_at` IS NULL
WHERE u.`deleted_at` IS NULL
  AND tp.`college_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM (
          SELECT `user_id`, `role_id` FROM `role_scopes` WHERE `deleted_at` IS NULL AND `status` = 1
      ) x WHERE x.`user_id` = u.`id` AND x.`role_id` = r.`id`
  );
