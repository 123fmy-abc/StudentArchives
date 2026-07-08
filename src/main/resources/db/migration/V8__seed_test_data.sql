-- ============================================================================
-- V8: 测试种子数据（Seed Test Data）
-- 描述：插入基础测试数据，用于开发阶段测试登录等接口
-- 注意：
--   1. 所有用户密码统一为 "123456"（BCrypt 加密）
--   2. 管理员账号 admin 密码为 "admin123"
--   3. 教师账号 teacher 密码为 "teacher123"
--   4. 仅用于开发/测试环境，生产环境请勿使用
-- ============================================================================

-- ============================================================================
-- 1. 基础数据
-- ============================================================================

-- 1.1 学校
INSERT INTO schools (id, name, code, status, created_at, updated_at) VALUES
(1, '测试大学', 'TEST_UNIV', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 1.2 学期
INSERT INTO semesters (id, name, start_date, end_date, is_current, status, created_at, updated_at) VALUES
(1, '2025-2026学年第二学期', '2026-02-15', '2026-07-15', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 1.3 能力维度
INSERT INTO ability_dimensions (id, dimension_name, dimension_code, description, sort, status, created_at, updated_at) VALUES
(1, '学术能力', 'academic', '学习成绩、科研成果等', 1, 1, NOW(), NOW()),
(2, '创新能力', 'innovation', '创新创业、竞赛等', 2, 1, NOW(), NOW()),
(3, '实践能力', 'practice', '社会实践、实习等', 3, 1, NOW(), NOW()),
(4, '综合素质', 'comprehensive', '组织管理、志愿等', 4, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE dimension_name = VALUES(dimension_name);

-- ============================================================================
-- 2. 组织架构
-- ============================================================================

-- 2.1 学院
INSERT INTO colleges (id, school_id, name, code, status, created_at, updated_at) VALUES
(1, 1, '计算机科学与技术学院', 'CS', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.2 专业
INSERT INTO majors (id, college_id, name, code, status, created_at, updated_at) VALUES
(1, 1, '计算机科学与技术', 'CS_MAJOR', 1, NOW(), NOW()),
(2, 1, '软件工程', 'SE_MAJOR', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.3 班级
INSERT INTO classes (id, major_id, name, grade, student_count, status, created_at, updated_at) VALUES
(1, 1, '计科2023-1班', '2023级', 30, 1, NOW(), NOW()),
(2, 2, '软件2023-1班', '2023级', 28, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================================
-- 3. 权限体系
-- ============================================================================

-- 3.1 角色（系统内置角色）
INSERT INTO roles (id, name, code, description, level, is_system, is_auditor, scope_types, max_scope_count, status, created_at, updated_at) VALUES
(1, '超级管理员', 'super_admin', '系统超级管理员，拥有所有权限', 0, 1, 1, '[1]', 0, 1, NOW(), NOW()),
(2, '学生', 'student', '普通学生用户', 1, 1, 0, '[4]', 1, 1, NOW(), NOW()),
(3, '教师', 'teacher', '教师用户', 2, 1, 0, '[2]', 1, 1, NOW(), NOW()),
(4, '辅导员', 'counselor', '辅导员', 3, 1, 1, '[4]', 5, 1, NOW(), NOW()),
(5, '系主任', 'dean', '系主任', 4, 1, 1, '[3]', 1, 1, NOW(), NOW()),
(6, '院长', 'college_head', '学院院长', 5, 1, 1, '[2]', 1, 1, NOW(), NOW()),
(7, '教务处管理员', 'edu_admin', '教务处管理员', 6, 1, 1, '[1]', 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3.2 权限（基础权限）
INSERT INTO permissions (id, name, code, type, parent_id, sort, status, created_at, updated_at) VALUES
-- 模块级权限（parent_id = NULL）
(1,  '档案管理', 'archive', 1, NULL, 1, 1, NOW(), NOW()),
(2,  '审批管理', 'approval', 1, NULL, 2, 1, NOW(), NOW()),
(3,  '用户管理', 'user', 1, NULL, 3, 1, NOW(), NOW()),
(4,  '系统设置', 'system', 1, NULL, 4, 1, NOW(), NOW()),
(5,  '综合测评', 'evaluation', 1, NULL, 5, 1, NOW(), NOW()),

-- 操作级权限
(10, '查看档案', 'archive:view', 2, 1, 1, 1, NOW(), NOW()),
(11, '创建档案', 'archive:create', 2, 1, 2, 1, NOW(), NOW()),
(12, '编辑档案', 'archive:edit', 2, 1, 3, 1, NOW(), NOW()),
(13, '删除档案', 'archive:delete', 2, 1, 4, 1, NOW(), NOW()),

(20, '审批档案', 'approval:approve', 2, 2, 1, 1, NOW(), NOW()),
(21, '查看审批', 'approval:view', 2, 2, 2, 1, NOW(), NOW()),

(30, '创建用户', 'user:create', 2, 3, 1, 1, NOW(), NOW()),
(31, '编辑用户', 'user:edit', 2, 3, 2, 1, NOW(), NOW()),
(32, '删除用户', 'user:delete', 2, 3, 3, 1, NOW(), NOW()),
(33, '查看用户', 'user:view', 2, 3, 4, 1, NOW(), NOW()),

(40, '系统配置', 'system:config', 2, 4, 1, 1, NOW(), NOW()),
(41, '角色管理', 'system:role', 2, 4, 2, 1, NOW(), NOW()),
(42, '权限管理', 'system:permission', 2, 4, 3, 1, NOW(), NOW()),

-- API 接口权限
(100, '档案-列表', 'api:archive:list', 3, NULL, 1, 1, NOW(), NOW()),
(101, '档案-详情', 'api:archive:detail', 3, NULL, 2, 1, NOW(), NOW()),
(102, '档案-提交', 'api:archive:submit', 3, NULL, 3, 1, NOW(), NOW()),
(103, '档案-删除', 'api:archive:delete', 3, NULL, 4, 1, NOW(), NOW()),
(200, '审批-通过', 'api:approval:pass', 3, NULL, 1, 1, NOW(), NOW()),
(201, '审批-驳回', 'api:approval:reject', 3, NULL, 2, 1, NOW(), NOW()),
(300, '用户-列表', 'api:user:list', 3, NULL, 1, 1, NOW(), NOW()),
(301, '用户-创建', 'api:user:create', 3, NULL, 2, 1, NOW(), NOW()),
(302, '用户-更新', 'api:user:update', 3, NULL, 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3.3 角色-权限关联（超级管理员绑定所有权限）
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 1, p.id, NOW(), NOW() FROM permissions p
WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- 学生角色绑定基础权限
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 2, p.id, NOW(), NOW() FROM permissions p
WHERE p.code IN ('archive:view', 'archive:create', 'archive:edit')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- 教师角色绑定相关权限
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 3, p.id, NOW(), NOW() FROM permissions p
WHERE p.code IN ('archive:view', 'approval:view', 'approval:approve', 'user:view')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);

-- ============================================================================
-- 4. 用户数据
-- ============================================================================

-- 4.1 管理员（用于后台登录）
-- 密码: admin123  (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (1, 1, 'admin', '系统管理员', 1, 'admin@test.edu.cn', '13800000001',
        '$2a$10$g.EieCiqEObzKF1PWxb3IuPG1rw4YkSdlO9cLOgvhFgsSJ8Bzp1K2',
        1, NOW(), NOW());

-- 4.2 辅导员（兼教师）
-- 密码: teacher123 (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (2, 1, 'T2023001', '张老师', 1, 'zhang@test.edu.cn', '13800000002',
        '$2a$10$6tPUCCWODkV3hOZetEAeZuhUhE33CIK9YAMY8298B4Kws5vNrzUIa',
        1, NOW(), NOW());

-- 4.3 教师
-- 密码: teacher123 (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (3, 1, 'T2023002', '李老师', 0, 'li@test.edu.cn', '13800000003',
        '$2a$10$6tPUCCWODkV3hOZetEAeZuhUhE33CIK9YAMY8298B4Kws5vNrzUIa',
        1, NOW(), NOW());

-- 4.4 学生1（男）
-- 密码: 123456 (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (4, 1, '202311001', '王小明', 1, 'wxm@test.edu.cn', '13800000004',
        '$2a$10$dE043DTeSoMtr61SoXqzw.CN6X9.s5NyAIp8NUU0ozEdT019WMLz.',
        1, NOW(), NOW());

-- 4.5 学生2（女）
-- 密码: 123456 (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (5, 1, '202311002', '李小红', 2, 'lxh@test.edu.cn', '13800000005',
        '$2a$10$dE043DTeSoMtr61SoXqzw.CN6X9.s5NyAIp8NUU0ozEdT019WMLz.',
        1, NOW(), NOW());

-- 4.6 学生3（被禁用的账号，用于测试禁用用户登录）
-- 密码: 123456 (BCrypt 加密)
INSERT IGNORE INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES (6, 1, '202311003', '赵禁用', 1, 'disabled@test.edu.cn', '13800000006',
        '$2a$10$dE043DTeSoMtr61SoXqzw.CN6X9.s5NyAIp8NUU0ozEdT019WMLz.',
        0, NOW(), NOW());

-- ============================================================================
-- 5. 用户角色关联
-- ============================================================================

-- 管理员 → 超级管理员角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (1, 1, NOW(), NOW());

-- 张老师 → 辅导员角色 + 教师角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (2, 4, NOW(), NOW()),
       (2, 3, NOW(), NOW());

-- 李老师 → 教师角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (3, 3, NOW(), NOW());

-- 王小明 → 学生角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (4, 2, NOW(), NOW());

-- 李小红 → 学生角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (5, 2, NOW(), NOW());

-- 赵禁用 → 学生角色
INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES (6, 2, NOW(), NOW());

-- ============================================================================
-- 6. 用户扩展信息
-- ============================================================================

-- 6.1 学生档案
INSERT IGNORE INTO student_profiles (user_id, class_id, political_status, volunteer_hours, language_ability, hobbies, created_at, updated_at)
VALUES (4, 1, '共青团员', 12.5, '英语（CET-4）', '篮球、编程', NOW(), NOW()),
       (5, 1, '群众', 8.0, '英语（CET-6）', '阅读、绘画', NOW(), NOW()),
       (6, 2, '共青团员', 0, NULL, NULL, NOW(), NOW());

-- 6.2 教师档案
INSERT IGNORE INTO teacher_profiles (user_id, college_id, title, created_at, updated_at)
VALUES (2, 1, '讲师', NOW(), NOW()),
       (3, 1, '副教授', NOW(), NOW());

-- ============================================================================
-- 7. 角色组织范围绑定
-- ============================================================================

-- 张老师（辅导员）绑定到班级
INSERT IGNORE INTO role_scopes (user_id, role_id, scope_type, scope_id, is_primary, appoint_by, valid_from, valid_until, status, created_at, updated_at)
VALUES (2, 4, 4, 1, 1, 1, '2026-02-15', NULL, 1, NOW(), NOW()),
       (2, 4, 4, 2, 0, 1, '2026-02-15', NULL, 1, NOW(), NOW());
