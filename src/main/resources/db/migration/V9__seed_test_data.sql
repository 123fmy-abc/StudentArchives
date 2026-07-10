-- ============================================================================
-- V9: 种子测试数据
-- 描述：生成少量但覆盖核心业务链的测试数据
--       密码统一为 "123456"（BCrypt 哈希）
--
-- 注意：V8 已通过 AUTO_INCREMENT 插入了审核员角色(roles)和
--       教师端权限(permissions)，因此本迁移的角色ID从10开始，
--       权限ID从100开始以避免冲突。
-- ============================================================================

-- ============================================================================
-- 1. 基础数据（Foundation）
-- ============================================================================

-- 1.1 学校
INSERT INTO schools (id, name, code, status, created_at, updated_at)
VALUES (1, '测试大学', 'TEST_UNIV', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 1.2 学期
INSERT INTO semesters (id, name, start_date, end_date, is_current, status, created_at, updated_at)
VALUES
    (1, '2024-2025学年第一学期', '2024-09-01', '2025-01-15', 0, 1, NOW(), NOW()),
    (2, '2024-2025学年第二学期', '2025-02-17', '2025-07-05', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 1.3 能力维度
INSERT INTO ability_dimensions (id, dimension_name, dimension_code, description, sort, status, created_at, updated_at)
VALUES
    (1, '学术能力',    'academic',       '学科竞赛、学术研究、奖学金等', 1, 1, NOW(), NOW()),
    (2, '创新实践',    'innovation',     '创新创业、实训项目等',         2, 1, NOW(), NOW()),
    (3, '社会责任',    'social',         '社会实践、志愿服务等',         3, 1, NOW(), NOW()),
    (4, '组织管理',    'organization',   '组织履历、社团活动等',         4, 1, NOW(), NOW()),
    (5, '综合素养',    'comprehensive',  '语言能力、兴趣特长等',         5, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE dimension_name = VALUES(dimension_name);

-- 1.4 数据字典
INSERT INTO dictionaries (id, dict_type, dict_code, dict_name, parent_id, sort, status, created_at, updated_at)
VALUES
    -- 政治面貌
    (1,  'political_status', 'party_member',  '中共党员',        NULL, 1, 1, NOW(), NOW()),
    (2,  'political_status', 'pre_party',     '中共预备党员',    NULL, 2, 1, NOW(), NOW()),
    (3,  'political_status', 'league_member', '共青团员',        NULL, 3, 1, NOW(), NOW()),
    (4,  'political_status', 'mass',          '群众',            NULL, 4, 1, NOW(), NOW()),
    -- 竞赛类型
    (5,  'competition_type', 'academic',      '学科竞赛',        NULL, 1, 1, NOW(), NOW()),
    (6,  'competition_type', 'innovate',      '创新创业大赛',    NULL, 2, 1, NOW(), NOW()),
    (7,  'competition_type', 'sports',        '体育竞赛',        NULL, 3, 1, NOW(), NOW()),
    -- 获奖等级
    (8,  'award_level',      'national',      '国家级',          NULL, 1, 1, NOW(), NOW()),
    (9,  'award_level',      'province',      '省级',            NULL, 2, 1, NOW(), NOW()),
    (10, 'award_level',      'school',        '校级',            NULL, 3, 1, NOW(), NOW()),
    -- 证书类型
    (11, 'certificate_type', 'lang',          '语言类证书',      NULL, 1, 1, NOW(), NOW()),
    (12, 'certificate_type', 'professional',  '职业资格证书',    NULL, 2, 1, NOW(), NOW()),
    -- 档案类型
    (13, 'archive_type',     'competition',    '学科竞赛',       NULL, 1, 1, NOW(), NOW()),
    (14, 'archive_type',     'scholarship',    '奖学金',         NULL, 2, 1, NOW(), NOW()),
    (15, 'archive_type',     'internship',     '实习经历',       NULL, 3, 1, NOW(), NOW()),
    (16, 'archive_type',     'social_practice','社会实践',       NULL, 4, 1, NOW(), NOW()),
    (17, 'archive_type',     'research',       '学术研究',       NULL, 5, 1, NOW(), NOW()),
    (18, 'archive_type',     'certificate',    '荣誉证书',       NULL, 6, 1, NOW(), NOW()),
    (19, 'archive_type',     'organization',   '组织履历',       NULL, 7, 1, NOW(), NOW()),
    (20, 'archive_type',     'training',       '实训项目',       NULL, 8, 1, NOW(), NOW()),
    (21, 'archive_type',     'innovation',     '创新创业',       NULL, 9, 1, NOW(), NOW()),
    (22, 'archive_type',     'book_review',    '图书心得',       NULL, 10, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

-- 1.5 系统配置
INSERT INTO system_settings (id, setting_key, setting_value, setting_group, value_type, description, is_editable, created_at, updated_at)
VALUES
    (1, 'school.name',      '测试大学',     'basic',    'string',  '学校名称',       0, NOW(), NOW()),
    (2, 'school.code',      'TEST_UNIV',    'basic',    'string',  '学校编码',       0, NOW(), NOW()),
    (3, 'semester.current', '2',            'academic', 'string',  '当前学期ID',     1, NOW(), NOW()),
    (4, 'archive.audit',    '1',            'archive',  'boolean', '启用档案审核',   1, NOW(), NOW()),
    (5, 'award.enabled',    '1',            'award',    'boolean', '启用奖项报名',   1, NOW(), NOW())
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 1.6 档案类型配置
INSERT INTO archive_type_config (id, archive_type, type_name, evaluate_desc, apply_desc, icon, sort, status, created_at, updated_at)
VALUES
    (1, 'competition',    '学科竞赛',  '记录学生在各类学科竞赛中的获奖情况', '请上传竞赛获奖证书',           'icon-competition',    1, 1, NOW(), NOW()),
    (2, 'scholarship',    '奖学金',    '记录学生获得的各类奖学金',         '请上传奖学金证书或证明文件',   'icon-scholarship',    2, 1, NOW(), NOW()),
    (3, 'internship',     '实习经历',  '记录学生的实习实践经历',           '请上传实习证明',               'icon-internship',     3, 1, NOW(), NOW()),
    (4, 'social_practice','社会实践',  '记录学生参与的社会实践活动',       '请上传实践证明材料',           'icon-practice',       4, 1, NOW(), NOW()),
    (5, 'research',       '学术研究',  '记录学生参与的学术研究项目',       '请上传项目证明材料',           'icon-research',       5, 1, NOW(), NOW()),
    (6, 'certificate',    '荣誉证书',  '记录学生获得的各类荣誉证书',       '请上传证书扫描件',             'icon-certificate',    6, 1, NOW(), NOW()),
    (7, 'organization',   '组织履历',  '记录学生在学生组织的任职情况',     '请上传任职证明',               'icon-organization',   7, 1, NOW(), NOW()),
    (8, 'training',       '实训项目',  '记录学生参与的实训项目',           '请上传项目成果材料',           'icon-training',       8, 1, NOW(), NOW()),
    (9, 'innovation',     '创新创业',  '记录学生的创新创业成果',           '请上传相关证明材料',           'icon-innovation',     9, 1, NOW(), NOW()),
    (10,'book_review',    '图书心得',  '记录学生的读书心得',               '请上传读书心得文章',           'icon-book',          10, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

-- ============================================================================
-- 2. 组织架构
-- ============================================================================

-- 2.1 学院
INSERT INTO colleges (id, school_id, name, code, status, created_at, updated_at)
VALUES (1, 1, '计算机科学与技术学院', 'CS', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.2 专业
INSERT INTO majors (id, college_id, name, code, status, created_at, updated_at)
VALUES (1, 1, '计算机科学与技术', 'CS_MAJOR', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2.3 班级
INSERT INTO classes (id, major_id, name, grade, student_count, status, created_at, updated_at)
VALUES (1, 1, '2024级计算机1班', '2024级', 30, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================================
-- 3. 角色与权限
-- 注意：ID 从 10/100 开始，避免与 V8 AUTO_INCREMENT 数据冲突
-- ============================================================================

-- 3.1 角色表（auditor 已在 V8 中插入）
INSERT INTO roles (id, name, code, description, level, is_system, is_auditor, scope_types, max_scope_count, status, created_at, updated_at)
VALUES
    (10, '超级管理员', 'super_admin', '系统超级管理员，拥有全部权限', 0,  1, 0, '[1]',      0, 1, NOW(), NOW()),
    (20, '学生',       'student',     '默认学生角色',                1,  1, 0, NULL,       0, 1, NOW(), NOW()),
    (30, '教师',       'teacher',     '默认教师角色',                2,  1, 0, '[2,3,4]',  0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3.2 权限表（基础权限，避开 V8 教师端权限的 AUTO_INCREMENT ID 范围 1-10）
INSERT INTO permissions (id, name, code, type, parent_id, sort, status, created_at, updated_at)
VALUES
    -- 系统管理（根菜单）
    (100, '系统管理',       'system:admin',     1, NULL, 1,  1, NOW(), NOW()),
    (101, '用户管理',       'system:user',      1, 100,  1,  1, NOW(), NOW()),
    (102, '角色管理',       'system:role',      1, 100,  2,  1, NOW(), NOW()),
    (103, '权限管理',       'system:permission', 1, 100,  3,  1, NOW(), NOW()),
    -- 档案管理（根菜单）
    (200, '档案管理',       'archive:admin',    1, NULL, 10, 1, NOW(), NOW()),
    (201, '我的档案',       'archive:my',       1, 200,  1,  1, NOW(), NOW()),
    (202, '档案审核',       'archive:audit',    1, 200,  2,  1, NOW(), NOW()),
    -- 奖项报名（根菜单）
    (300, '奖项报名',       'award:admin',      1, NULL, 20, 1, NOW(), NOW()),
    (301, '我的申报',       'award:my',         1, 300,  1,  1, NOW(), NOW()),
    (302, '奖项审核',       'award:audit',      1, 300,  2,  1, NOW(), NOW()),
    -- 成绩管理（根菜单）
    (400, '成绩管理',       'grade:admin',      1, NULL, 30, 1, NOW(), NOW()),
    (401, '成绩查询',       'grade:view',       1, 400,  1,  1, NOW(), NOW()),
    (402, '成绩导入',       'grade:import',     1, 400,  2,  1, NOW(), NOW()),
    -- 公共基础（根菜单）
    (500, '仪表盘',         'dashboard',        1, NULL, 0,  1, NOW(), NOW()),
    (501, '个人中心',       'profile',          1, NULL, 99, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3.3 角色-权限关联
-- 超级管理员（role_id=10）：全部权限
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 10, id, NOW(), NOW() FROM permissions
WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 10 AND rp.permission_id = permissions.id);

-- 学生（role_id=20）：档案 + 奖项报名 + 成绩查询 + 仪表盘 + 个人中心
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 20, id, NOW(), NOW() FROM permissions p
WHERE p.code IN ('dashboard', 'profile', 'archive:admin', 'archive:my', 'award:admin', 'award:my', 'grade:admin', 'grade:view')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 20 AND rp.permission_id = p.id);

-- 教师（role_id=30）：档案审核 + 奖项审核 + 成绩管理等
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT 30, id, NOW(), NOW() FROM permissions p
WHERE p.code IN ('dashboard', 'profile', 'archive:admin', 'archive:audit', 'award:admin', 'award:audit', 'grade:admin', 'grade:view', 'grade:import')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 30 AND rp.permission_id = p.id);

-- ============================================================================
-- 4. 用户数据
-- ============================================================================
-- 密码说明：所有用户密码均为 "123456"，BCrypt 哈希值：
-- $2a$10$nhrl7QKIb7rHTzEe0V/YEe3HtaK.ktJtQXHwNzu.v4dSR7bBgFRhW（bcryptjs 生成，已验证匹配 "123456"）
-- ============================================================================

-- 4.1 用户表
INSERT INTO users (id, school_id, user_no, name, gender, email, phone, password, status, created_at, updated_at)
VALUES
    -- 超级管理员
    (1, 1, 'admin',     '系统管理员', 1, 'admin@test.edu.cn',     '13800000001',
     '$2a$10$nhrl7QKIb7rHTzEe0V/YEe3HtaK.ktJtQXHwNzu.v4dSR7bBgFRhW', 1, NOW(), NOW()),
    -- 教师
    (2, 1, 'T2024001',  '张教授',     1, 'zhang@test.edu.cn',     '13800000002',
     '$2a$10$nhrl7QKIb7rHTzEe0V/YEe3HtaK.ktJtQXHwNzu.v4dSR7bBgFRhW', 1, NOW(), NOW()),
    -- 学生
    (3, 1, 'S2024001',  '李明',       1, 'liming@test.edu.cn',    '13800000003',
     '$2a$10$nhrl7QKIb7rHTzEe0V/YEe3HtaK.ktJtQXHwNzu.v4dSR7bBgFRhW', 1, NOW(), NOW()),
    (4, 1, 'S2024002',  '王芳',       2, 'wangfang@test.edu.cn',  '13800000004',
     '$2a$10$nhrl7QKIb7rHTzEe0V/YEe3HtaK.ktJtQXHwNzu.v4dSR7bBgFRhW', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 4.2 用户-角色关联
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
VALUES
    (1, 10, NOW(), NOW()),  -- admin → 超级管理员
    (2, 30, NOW(), NOW()),  -- 张教授 → 教师
    (3, 20, NOW(), NOW()),  -- 李明 → 学生
    (4, 20, NOW(), NOW())   -- 王芳 → 学生
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4.3 角色组织范围绑定（为教师绑定到计算机学院）
INSERT INTO role_scopes (user_id, role_id, scope_type, scope_id, is_primary, appoint_by, appoint_reason, valid_from, valid_until, status, created_at, updated_at)
VALUES
    (2, 30, 2, 1, 1, 1, '默认教师范围', '2024-09-01', NULL, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4.4 学生档案扩展
INSERT INTO student_profiles (user_id, class_id, political_status, volunteer_hours, language_ability, hobbies, created_at, updated_at)
VALUES
    (3, 1, '共青团员', 24.50, '英语（CET-4）', '篮球、编程',  NOW(), NOW()),
    (4, 1, '共青团员', 36.00, '英语（CET-6）', '阅读、绘画',  NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4.5 教师档案扩展
INSERT INTO teacher_profiles (user_id, college_id, title, created_at, updated_at)
VALUES (2, 1, '教授', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4.6 用户兴趣
INSERT INTO user_interests (user_id, tag_name, proficiency_level, weight, sort, is_detail, detail_content, created_at, updated_at)
VALUES
    (3, '编程',     4, 10, 1, 0, NULL,         NOW(), NOW()),
    (3, '算法竞赛', 3, 8,  2, 1, 'ACM竞赛',    NOW(), NOW()),
    (4, '英语',     4, 12, 1, 0, NULL,         NOW(), NOW()),
    (4, '绘画',     3, 6,  2, 1, '水彩画',     NOW(), NOW())
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- 4.7 通知设置
INSERT INTO notification_settings (user_id, category, email_enabled, sms_enabled, push_enabled, created_at, updated_at)
VALUES
    (3, 'audit_remind',    1, 0, 1, NOW(), NOW()),
    (3, 'system_notice',   1, 0, 1, NOW(), NOW()),
    (3, 'dynamic_remind',  0, 0, 1, NOW(), NOW()),
    (4, 'audit_remind',    1, 0, 1, NOW(), NOW()),
    (4, 'system_notice',   1, 0, 1, NOW(), NOW()),
    (4, 'dynamic_remind',  0, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE push_enabled = VALUES(push_enabled);

-- ============================================================================
-- 5. 档案数据（示例）
-- ============================================================================

-- 5.1 档案基表
INSERT INTO archives (id, school_id, user_id, archive_type, title, semester_id, obtain_time, status, submit_time, audited_at, auditor_id, current_version, submit_count, created_at, updated_at)
VALUES
    -- 李明（user_id=3）— 已审核通过
    (1, 1, 3, 'competition', '2024年全国大学生程序设计竞赛省二等奖',        1, '2024-11-15', 2, '2024-11-20 10:00:00', '2024-11-22 14:00:00', 2, 1, 1, NOW(), NOW()),
    (2, 1, 3, 'scholarship', '2023-2024学年国家奖学金',                     1, '2024-10-01', 2, '2024-10-10 09:00:00', '2024-10-15 16:00:00', 2, 1, 1, NOW(), NOW()),
    -- 李明 — 草稿
    (3, 1, 3, 'internship',  '字节跳动前端开发实习生',                      2, '2025-03-01', 0, NULL,                     NULL,                  NULL, 1, 0, NOW(), NOW()),
    -- 王芳（user_id=4）— 已审核通过
    (4, 1, 4, 'certificate', '大学英语六级证书（CET-6）',                   1, '2024-12-15', 2, '2024-12-20 11:00:00', '2024-12-22 09:00:00', 2, 1, 1, NOW(), NOW()),
    -- 王芳 — 待审批
    (5, 1, 4, 'social_practice','2024年暑期社区志愿服务',                   1, '2024-08-20', 1, '2024-09-01 08:00:00', NULL,                  NULL, 1, 1, NOW(), NOW()),
    (6, 1, 4, 'research',     '基于深度学习的图像识别研究',                 1, '2024-12-01', 1, '2024-12-10 14:00:00', NULL,                  NULL, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 5.2 档案扩展数据
-- 竞赛
INSERT INTO archive_competitions (archive_id, competition_name, competition_type, award_level, created_at, updated_at)
VALUES (1, '全国大学生程序设计竞赛', 'academic', '省级二等奖', NOW(), NOW())
ON DUPLICATE KEY UPDATE competition_name = VALUES(competition_name);

-- 奖学金
INSERT INTO archive_scholarships (archive_id, scholarship_name, scholarship_category, award_level, created_at, updated_at)
VALUES (2, '国家奖学金', '国家', '一等奖', NOW(), NOW())
ON DUPLICATE KEY UPDATE scholarship_name = VALUES(scholarship_name);

-- 实习
INSERT INTO archive_internships (archive_id, company_name, location, position, start_date, end_date, created_at, updated_at)
VALUES (3, '字节跳动', '北京', '前端开发实习生', '2025-03-01', '2025-06-30', NOW(), NOW())
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

-- 证书
INSERT INTO archive_certificates (archive_id, certificate_type, certificate_name, created_at, updated_at)
VALUES (4, 'lang', '大学英语六级证书（CET-6）', NOW(), NOW())
ON DUPLICATE KEY UPDATE certificate_name = VALUES(certificate_name);

-- 社会实践
INSERT INTO archive_social_practices (archive_id, activity_name, practice_location, practice_unit, start_date, end_date, volunteer_hours, created_at, updated_at)
VALUES (5, '暑期社区志愿服务', '阳光社区', '阳光社区居委会', '2024-07-15', '2024-08-20', 36.00, NOW(), NOW())
ON DUPLICATE KEY UPDATE activity_name = VALUES(activity_name);

-- 学术研究
INSERT INTO archive_researches (archive_id, project_name, project_level, project_type, team_role, project_time, created_at, updated_at)
VALUES (6, '基于深度学习的图像识别研究', '校级', '大学生创新训练项目', '项目负责人', '2024-09', NOW(), NOW())
ON DUPLICATE KEY UPDATE project_name = VALUES(project_name);

-- 5.3 档案版本快照
INSERT INTO archive_versions (archive_id, version, title, data_snapshot, status, rejected_reason, created_at)
VALUES
    (1, 1, '2024年全国大学生程序设计竞赛省二等奖',
     '{"archive_type":"competition","competition_name":"全国大学生程序设计竞赛","competition_type":"academic","award_level":"省级二等奖","obtain_time":"2024-11-15"}',
     2, NULL, NOW()),
    (2, 1, '2023-2024学年国家奖学金',
     '{"archive_type":"scholarship","scholarship_name":"国家奖学金","scholarship_category":"国家","award_level":"一等奖","obtain_time":"2024-10-01"}',
     2, NULL, NOW()),
    (4, 1, '大学英语六级证书（CET-6）',
     '{"archive_type":"certificate","certificate_type":"lang","certificate_name":"大学英语六级证书（CET-6）","obtain_time":"2024-12-15"}',
     2, NULL, NOW());

-- ============================================================================
-- 6. 成绩数据
-- ============================================================================

-- 6.1 GPA 记录 — 李明
INSERT INTO gpa_records (id, school_id, user_id, semester_id, course_code, course_name, attempt_no, score, gpa, credit, created_at, updated_at)
VALUES
    (1,  1, 3, 1, 'CS101', '高等数学A（上）',    1, 92.00, 4.50, 5.0, NOW(), NOW()),
    (2,  1, 3, 1, 'CS102', '线性代数',           1, 88.00, 4.00, 4.0, NOW(), NOW()),
    (3,  1, 3, 1, 'CS103', 'C语言程序设计',      1, 95.00, 5.00, 4.0, NOW(), NOW()),
    (4,  1, 3, 1, 'CS104', '大学英语（一）',     1, 85.00, 3.70, 4.0, NOW(), NOW()),
    (5,  1, 3, 1, 'CS105', '思想政治（一）',     1, 90.00, 4.30, 3.0, NOW(), NOW()),
    (6,  1, 3, 2, 'CS201', '高等数学A（下）',    1, 89.00, 4.10, 5.0, NOW(), NOW()),
    (7,  1, 3, 2, 'CS202', '离散数学',           1, 86.00, 3.80, 4.0, NOW(), NOW()),
    (8,  1, 3, 2, 'CS203', 'Java面向对象编程',   1, 93.00, 4.60, 4.0, NOW(), NOW()),
    (9,  1, 3, 2, 'CS204', '大学英语（二）',     1, 82.00, 3.50, 4.0, NOW(), NOW()),
    (10, 1, 3, 2, 'CS205', '思想政治（二）',     1, 88.00, 4.00, 3.0, NOW(), NOW())
ON DUPLICATE KEY UPDATE score = VALUES(score);

-- 6.2 GPA 记录 — 王芳
INSERT INTO gpa_records (id, school_id, user_id, semester_id, course_code, course_name, attempt_no, score, gpa, credit, created_at, updated_at)
VALUES
    (11, 1, 4, 1, 'CS101', '高等数学A（上）',    1, 95.00, 5.00, 5.0, NOW(), NOW()),
    (12, 1, 4, 1, 'CS102', '线性代数',           1, 91.00, 4.40, 4.0, NOW(), NOW()),
    (13, 1, 4, 1, 'CS103', 'C语言程序设计',      1, 88.00, 4.00, 4.0, NOW(), NOW()),
    (14, 1, 4, 1, 'CS104', '大学英语（一）',     1, 96.00, 5.00, 4.0, NOW(), NOW()),
    (15, 1, 4, 1, 'CS105', '思想政治（一）',     1, 92.00, 4.50, 3.0, NOW(), NOW()),
    (16, 1, 4, 2, 'CS201', '高等数学A（下）',    1, 93.00, 4.60, 5.0, NOW(), NOW()),
    (17, 1, 4, 2, 'CS202', '离散数学',           1, 90.00, 4.30, 4.0, NOW(), NOW()),
    (18, 1, 4, 2, 'CS203', 'Java面向对象编程',   1, 91.00, 4.40, 4.0, NOW(), NOW()),
    (19, 1, 4, 2, 'CS204', '大学英语（二）',     1, 94.00, 4.80, 4.0, NOW(), NOW()),
    (20, 1, 4, 2, 'CS205', '思想政治（二）',     1, 90.00, 4.30, 3.0, NOW(), NOW())
ON DUPLICATE KEY UPDATE score = VALUES(score);

-- 6.3 学期成绩汇总
INSERT INTO semester_gpa_summaries (user_id, semester_id, course_count, total_credit, weighted_gpa, average_score, rank_in_class, rank_in_major, created_at, updated_at)
VALUES
    (3, 1, 5, 20.0, 4.29, 90.00, 2, 5, NOW(), NOW()),
    (3, 2, 5, 20.0, 4.01, 87.60, 3, 8, NOW(), NOW()),
    (4, 1, 5, 20.0, 4.57, 92.40, 1, 2, NOW(), NOW()),
    (4, 2, 5, 20.0, 4.49, 91.60, 1, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE weighted_gpa = VALUES(weighted_gpa);

-- 6.4 画像评估分数
INSERT INTO portrait_evaluation_scores (user_id, semester_id, dimension_code, score, target_score, change_value, gap, evaluated_at, created_at, updated_at)
VALUES
    -- 李明 - 第一学期
    (3, 1, 'academic',      82.00, 90.00, 0.00, 8.00,  NOW(), NOW(), NOW()),
    (3, 1, 'innovation',    70.00, 80.00, 0.00, 10.00, NOW(), NOW(), NOW()),
    (3, 1, 'social',        60.00, 75.00, 0.00, 15.00, NOW(), NOW(), NOW()),
    (3, 1, 'organization',  55.00, 70.00, 0.00, 15.00, NOW(), NOW(), NOW()),
    (3, 1, 'comprehensive', 75.00, 85.00, 0.00, 10.00, NOW(), NOW(), NOW()),
    -- 王芳 - 第一学期
    (4, 1, 'academic',      90.00, 95.00, 0.00, 5.00,  NOW(), NOW(), NOW()),
    (4, 1, 'innovation',    65.00, 80.00, 0.00, 15.00, NOW(), NOW(), NOW()),
    (4, 1, 'social',        80.00, 85.00, 0.00, 5.00,  NOW(), NOW(), NOW()),
    (4, 1, 'organization',  60.00, 75.00, 0.00, 15.00, NOW(), NOW(), NOW()),
    (4, 1, 'comprehensive', 88.00, 90.00, 0.00, 2.00,  NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE score = VALUES(score);

-- ============================================================================
-- 7. 奖项报名数据
-- ============================================================================

-- 7.1 奖项类型配置
INSERT INTO award_type_config (id, award_type, type_name, evaluate_desc, apply_desc, icon, sort, status, created_at, updated_at)
VALUES
    (1, 'competition_star', '竞赛之星', '表彰在学科竞赛中取得突出成绩的学生', '请填写竞赛获奖信息', 'icon-star-competition', 1, 1, NOW(), NOW()),
    (2, 'research_star',    '科研之星', '表彰在学术研究方面表现突出的学生',   '请填写科研成果信息',  'icon-star-research',    2, 1, NOW(), NOW()),
    (3, 'innovation_star',  '双创之星', '表彰在创新创业方面表现突出的学生',   '请填写创业或创新信息', 'icon-star-innovation', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

-- 7.2 奖项报名 — 李明申报竞赛之星（待审批）
INSERT INTO award_applications (id, school_id, user_id, award_type, title, semester_id, status, submit_time, current_version, submit_count, created_at, updated_at)
VALUES (1, 1, 3, 'competition_star', '2024年度竞赛之星申报', 1, 1, '2024-12-01 10:00:00', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO app_competition_stars (application_id, competition_name, participate_time, competition_level, award_level, created_at, updated_at)
VALUES (1, '全国大学生程序设计竞赛', '2024-11-15', '省级', '二等奖', NOW(), NOW())
ON DUPLICATE KEY UPDATE competition_name = VALUES(competition_name);

-- 7.3 奖项报名 — 王芳申报科研之星（已通过）
INSERT INTO award_applications (id, school_id, user_id, award_type, title, semester_id, status, submit_time, audited_at, auditor_id, current_version, submit_count, created_at, updated_at)
VALUES (2, 1, 4, 'research_star', '2024年度科研之星申报', 1, 2, '2024-12-05 09:00:00', '2024-12-10 15:00:00', 2, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO app_research_stars (application_id, created_at, updated_at)
VALUES (2, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO app_research_projects (research_star_id, project_name, project_level, rank_total, establish_time, created_at, updated_at)
VALUES (1, '基于深度学习的图像识别研究', '校级', '1/5', '2024-09-01', NOW(), NOW())
ON DUPLICATE KEY UPDATE project_name = VALUES(project_name);

-- ============================================================================
-- 8. 教师授课与课程
-- ============================================================================

INSERT INTO teacher_courses (id, school_id, teacher_id, class_id, course_code, course_name, semester_id, is_primary, status, created_at, updated_at)
VALUES
    (1, 1, 2, 1, 'CS203', 'Java面向对象编程', 2, 1, 1, NOW(), NOW()),
    (2, 1, 2, 1, 'CS103', 'C语言程序设计',    1, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name);

-- ============================================================================
-- 9. 短板识别与改进建议
-- ============================================================================

INSERT INTO weakness_analyses (id, user_id, weakness_type, weakness_desc, source, severity_level, target_score, is_read, created_at, updated_at)
VALUES
    (1, 3, '组织管理能力不足', '在学生组织和活动参与方面较少，缺乏组织管理经验', 2, 3, 70, 0, NOW(), NOW()),
    (2, 4, '创新创业能力有待提升', '在创新创业实践方面参与较少，需要加强实践锻炼', 1, 2, 80, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE weakness_desc = VALUES(weakness_desc);

INSERT INTO improvement_suggestions (weakness_id, suggestion_content, source, is_implemented, created_at, updated_at)
VALUES
    (1, '建议积极参加学生社团或班级管理工作，锻炼组织协调能力', 2, 0, NOW(), NOW()),
    (2, '建议关注学校创新创业项目征集通知，积极参与大创项目',     1, 0, NOW(), NOW());

-- ============================================================================
-- 10. 消息与公告
-- ============================================================================

-- 10.1 公告
INSERT INTO announcements (id, title, content, publisher_id, target_type, target_id, publish_time, status, created_at, updated_at)
VALUES
    (1, '关于2024-2025学年第二学期档案填报的通知',
     '各位同学：\n\n2024-2025学年第二学期档案填报工作已启动，请各位同学在2025年3月31日前完成本学期档案资料的更新和提交。\n\n如有疑问，请联系辅导员或教务处。',
     1, 'all', NULL, '2025-02-20 08:00:00', 1, NOW(), NOW()),
    (2, '2024年度竞赛之星评选通知',
     '各位同学：\n\n2024年度竞赛之星评选现已开始接受报名，请符合条件的同学于2025年1月15日前提交申报材料。\n\n教务处',
     1, 'all', NULL, '2025-01-01 09:00:00', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 10.2 公告已读
INSERT INTO announcement_reads (announcement_id, user_id, created_at, updated_at)
VALUES
    (1, 3, NOW(), NOW()),
    (1, 4, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================================
-- 11. 成长时间轴
-- ============================================================================

INSERT INTO growth_timelines (id, school_id, user_id, semester_id, event_type, event_name, content, event_time, source_id, source_type, created_at, updated_at)
VALUES
    (1, 1, 3, 1, 1, '全国大学生程序设计竞赛省级二等奖',
     '在2024年全国大学生程序设计竞赛中荣获省级二等奖', '2024-11-15', 1, 'archive', NOW(), NOW()),
    (2, 1, 4, 1, 1, '大学英语六级通过',
     '通过大学英语六级考试（CET-6），成绩优秀', '2024-12-15', 4, 'archive', NOW(), NOW()),
    (3, 1, 4, 1, 3, '暑期社区志愿服务',
     '参与阳光社区暑期志愿服务活动，累计志愿时长36小时', '2024-08-20', 5, 'archive', NOW(), NOW())
ON DUPLICATE KEY UPDATE event_name = VALUES(event_name);
