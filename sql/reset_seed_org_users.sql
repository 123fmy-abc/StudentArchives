-- ============================================================
-- 重置种子数据：组织架构 + 用户 + 简化角色（学生/辅导员/系主任/院长）
--
-- 本脚本会先删除上一批种子数据（id >= 101 的组织、用户、角色），
-- 再按「学生 → 辅导员(班级) → 系主任(专业) → 院长(学院)」的审批链
-- 重建一套干净的联调数据。历史数据（users id 1~8、colleges/majors/classes
-- id 1~2、archives 等）不受影响。
--
-- 角色体系（只保留 6 个）：
--   1 学生 student / 2 教师 teacher / 3 管理员 admin（系统内置）
--   117 辅导员 counselor        level=3 scope_types=[4] 班级
--   118 系主任 department_head  level=4 scope_types=[3] 专业
--   119 院长   college_dean     level=5 scope_types=[2] 学院
--
-- ID 规划：
--   colleges 101~102 / majors 101~103 / classes 101~104
--   users 101~120 / student_profiles 101~104 / teacher_profiles 105~120
--   user_roles 101~120 / role_scopes 101~117 / user_contact_infos 101~120
--
-- 登录密码统一 123456
-- 可重复执行：先清理后插入，最终结果一致。
-- ============================================================

USE `student_archives`;

START TRANSACTION;

-- ============================================================
-- 0. 清理上一批种子数据（只删 id >= 101 的种子记录）
-- ============================================================
DELETE FROM `role_permissions`   WHERE `role_id` BETWEEN 101 AND 200;
DELETE FROM `user_roles`         WHERE `user_id` BETWEEN 101 AND 200 OR `role_id` BETWEEN 101 AND 200;
DELETE FROM `role_scopes`        WHERE `user_id` BETWEEN 101 AND 200 OR `role_id` BETWEEN 101 AND 200;
DELETE FROM `student_profiles`   WHERE `user_id` BETWEEN 101 AND 200;
DELETE FROM `teacher_profiles`   WHERE `user_id` BETWEEN 101 AND 200;
DELETE FROM `user_contact_infos` WHERE `user_id` BETWEEN 101 AND 200;

DELETE FROM `users`     WHERE `id` BETWEEN 101 AND 200;
DELETE FROM `classes`   WHERE `id` BETWEEN 101 AND 200;
DELETE FROM `majors`    WHERE `id` BETWEEN 101 AND 200;
DELETE FROM `colleges`  WHERE `id` BETWEEN 101 AND 200;
DELETE FROM `roles`     WHERE `id` BETWEEN 101 AND 200;

-- ============================================================
-- 1. 简化角色：辅导员 / 系主任 / 院长
-- ============================================================
INSERT INTO `roles`
(`id`, `name`, `code`, `description`, `level`, `role_type`, `is_system`, `is_auditor`, `scope_types`, `max_scope_count`, `status`) VALUES
(117, '辅导员', 'counselor',        '班级辅导员，审核所辖班级学生的申报',       3, 3, 0, 1, '[4]', 0, 1),
(118, '系主任', 'department_head',  '专业系主任，审核本专业学生申报',           4, 3, 0, 1, '[3]', 0, 1),
(119, '院长',   'college_dean',     '学院院长，审核本学院学生申报',             5, 3, 0, 1, '[2]', 0, 1);

SET @school_id   = (SELECT `id` FROM `schools` WHERE `code` = 'DEMO_UNIV' AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);
SET @student_role = (SELECT `id` FROM `roles` WHERE `code` = 'student'         AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);
SET @teacher_role = (SELECT `id` FROM `roles` WHERE `code` = 'teacher'         AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);
SET @counselor    = (SELECT `id` FROM `roles` WHERE `code` = 'counselor'       AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);
SET @dept_head    = (SELECT `id` FROM `roles` WHERE `code` = 'department_head' AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);
SET @dean         = (SELECT `id` FROM `roles` WHERE `code` = 'college_dean'    AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1);

-- ============================================================
-- 2. 学院 / 专业 / 班级
-- ============================================================
INSERT INTO `colleges` (`id`, `school_id`, `name`, `code`, `status`) VALUES
(101, @school_id, '信息工程学院', 'IE', 1),
(102, @school_id, '经济管理学院', 'EM', 1);

INSERT INTO `majors` (`id`, `college_id`, `name`, `code`, `status`) VALUES
(101, 101, '通信工程',     'CE', 1),
(102, 101, '电子信息工程', 'EE', 1),
(103, 102, '工商管理',     'BA', 1);

INSERT INTO `classes` (`id`, `major_id`, `name`, `grade`, `student_count`, `status`) VALUES
(101, 101, '通信工程2024级1班',     '2024级', 1, 1),
(102, 101, '通信工程2024级2班',     '2024级', 1, 1),
(103, 102, '电子信息工程2024级1班', '2024级', 1, 1),
(104, 103, '工商管理2024级1班',     '2024级', 1, 1);

-- ============================================================
-- 3. 用户（密码统一 123456）
--    101~104 学生 / 105~106 教师 / 107~112 辅导员
--    113~117 系主任 / 118~120 院长
-- ============================================================
INSERT INTO `users` (`id`, `school_id`, `user_no`, `name`, `gender`, `birth_date`, `password`, `status`) VALUES
(101, @school_id, '202410101', '林晓',   2, '2006-03-12', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(102, @school_id, '202410102', '周洋',   1, '2005-11-30', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(103, @school_id, '202410103', '吴敏',   2, '2006-07-08', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(104, @school_id, '202410104', '郑凯',   1, '2006-01-19', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(105, @school_id, 'T2024101',  '许文博', 1, '1979-06-08', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(106, @school_id, 'T2024102',  '何静',   2, '1986-10-21', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(107, @school_id, 'C2024101',  '李文静', 2, '1988-05-14', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(108, @school_id, 'C2024102',  '陈志远', 1, '1987-09-23', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(109, @school_id, 'C2024103',  '赵雅琴', 2, '1990-03-08', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(110, @school_id, 'C2024104',  '孙浩',   1, '1992-11-17', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(111, @school_id, 'C2024105',  '周丽',   2, '1991-07-02', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(112, @school_id, 'C2024106',  '马超',   1, '1989-12-25', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(113, @school_id, 'D2024101',  '王国安', 1, '1972-04-16', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(114, @school_id, 'D2024102',  '刘慧芳', 2, '1975-09-02', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(115, @school_id, 'D2024103',  '陈建国', 1, '1970-12-08', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(116, @school_id, 'D2024104',  '张伟明', 1, '1968-06-25', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(117, @school_id, 'D2024105',  '李国强', 1, '1971-08-19', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(118, @school_id, 'M2024101',  '赵丽华', 2, '1973-03-14', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(119, @school_id, 'M2024102',  '孙文华', 1, '1969-11-05', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(120, @school_id, 'M2024103',  '周美玲', 2, '1974-06-28', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1);

-- ============================================================
-- 4. 学生档案（一人一班）
-- ============================================================
INSERT INTO `student_profiles`
(`id`, `user_id`, `class_id`, `political_status`, `student_status`, `degree_type`, `volunteer_hours`) VALUES
(101, 101, 101, 'league_member',    'current', 'undergraduate',  96.00),
(102, 102, 102, 'masses',           'current', 'undergraduate',  60.50),
(103, 103, 103, 'party_member_pre', 'current', 'undergraduate', 150.00),
(104, 104, 104, 'league_member',    'current', 'undergraduate',  88.00);

-- ============================================================
-- 5. 教师档案（教师 / 辅导员 / 系主任 / 院长）
-- ============================================================
INSERT INTO `teacher_profiles` (`id`, `user_id`, `college_id`, `title`) VALUES
(105, 105, 101, '副教授'),
(106, 106, 102, '讲师'),
(107, 107,   1, '辅导员'),
(108, 108,   1, '辅导员'),
(109, 109, 101, '辅导员'),
(110, 110, 101, '辅导员'),
(111, 111, 101, '辅导员'),
(112, 112, 102, '辅导员'),
(113, 113,   1, '系主任'),
(114, 114,   1, '系主任'),
(115, 115, 101, '系主任'),
(116, 116, 101, '系主任'),
(117, 117, 102, '系主任'),
(118, 118,   1, '院长'),
(119, 119, 101, '院长'),
(120, 120, 102, '院长');

-- ============================================================
-- 6. 用户-角色关联（每人一个角色）
-- ============================================================
INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(101, 101, @student_role), (102, 102, @student_role),
(103, 103, @student_role), (104, 104, @student_role),
(105, 105, @teacher_role), (106, 106, @teacher_role),
(107, 107, @counselor),    (108, 108, @counselor),
(109, 109, @counselor),    (110, 110, @counselor),
(111, 111, @counselor),    (112, 112, @counselor),
(113, 113, @dept_head),    (114, 114, @dept_head),
(115, 115, @dept_head),    (116, 116, @dept_head),
(117, 117, @dept_head),
(118, 118, @dean),         (119, 119, @dean), (120, 120, @dean);

-- ============================================================
-- 7. 审批范围
--    辅导员 → 班级(4)：班 1、2、101、102、103、104
--    系主任 → 专业(3)：专业 1、2、101、102、103
--    院长   → 学院(2)：学院 1、101、102
--    教师   → 学院/班级（只读查看用，非审批）
-- ============================================================
INSERT INTO `role_scopes`
(`id`, `school_id`, `user_id`, `role_id`, `scope_type`, `scope_id`, `semester_id`,
 `is_primary`, `appoint_by`, `appoint_reason`, `valid_from`, `valid_until`, `status`) VALUES
(101, @school_id, 105, @teacher_role, 2, 101, NULL, 1, NULL, '联调数据：许文博 信息工程学院', '2024-09-01', NULL, 1),
(102, @school_id, 105, @teacher_role, 4, 101, NULL, 1, NULL, '联调数据：许文博 通信工程2024级1班', '2024-09-01', NULL, 1),
(103, @school_id, 106, @teacher_role, 2, 102, NULL, 1, NULL, '联调数据：何静 经济管理学院', '2024-09-01', NULL, 1),
(104, @school_id, 107, @counselor,    4,   1, NULL, 1, NULL, '联调数据：李文静 软件工程2023级1班', '2024-09-01', NULL, 1),
(105, @school_id, 108, @counselor,    4,   2, NULL, 1, NULL, '联调数据：陈志远 软工25201', '2024-09-01', NULL, 1),
(106, @school_id, 109, @counselor,    4, 101, NULL, 1, NULL, '联调数据：赵雅琴 通信工程2024级1班', '2024-09-01', NULL, 1),
(107, @school_id, 110, @counselor,    4, 102, NULL, 1, NULL, '联调数据：孙浩 通信工程2024级2班', '2024-09-01', NULL, 1),
(108, @school_id, 111, @counselor,    4, 103, NULL, 1, NULL, '联调数据：周丽 电子信息工程2024级1班', '2024-09-01', NULL, 1),
(109, @school_id, 112, @counselor,    4, 104, NULL, 1, NULL, '联调数据：马超 工商管理2024级1班', '2024-09-01', NULL, 1),
(110, @school_id, 113, @dept_head,    3,   1, NULL, 1, NULL, '联调数据：王国安 软件工程系主任', '2024-09-01', NULL, 1),
(111, @school_id, 114, @dept_head,    3,   2, NULL, 1, NULL, '联调数据：刘慧芳 计算机科学与技术系主任', '2024-09-01', NULL, 1),
(112, @school_id, 115, @dept_head,    3, 101, NULL, 1, NULL, '联调数据：陈建国 通信工程系主任', '2024-09-01', NULL, 1),
(113, @school_id, 116, @dept_head,    3, 102, NULL, 1, NULL, '联调数据：张伟明 电子信息工程系主任', '2024-09-01', NULL, 1),
(114, @school_id, 117, @dept_head,    3, 103, NULL, 1, NULL, '联调数据：李国强 工商管理系主任', '2024-09-01', NULL, 1),
(115, @school_id, 118, @dean,         2,   1, NULL, 1, NULL, '联调数据：赵丽华 计算机学院院长', '2024-09-01', NULL, 1),
(116, @school_id, 119, @dean,         2, 101, NULL, 1, NULL, '联调数据：孙文华 信息工程学院院长', '2024-09-01', NULL, 1),
(117, @school_id, 120, @dean,         2, 102, NULL, 1, NULL, '联调数据：周美玲 经济管理学院院长', '2024-09-01', NULL, 1);

-- ============================================================
-- 8. 联系信息
-- ============================================================
INSERT INTO `user_contact_infos` (`id`, `user_id`, `phone`, `email`, `address`) VALUES
(101, 101, '13800001101', 'linxiao@demo.edu.cn',    '示例大学东区宿舍1栋101室'),
(102, 102, '13800001102', 'zhouyang@demo.edu.cn',   '示例大学东区宿舍1栋102室'),
(103, 103, '13800001103', 'wumin@demo.edu.cn',      '示例大学东区宿舍1栋103室'),
(104, 104, '13800001104', 'zhengkai@demo.edu.cn',   '示例大学东区宿舍1栋104室'),
(105, 105, '13700001105', 'xuwenbo@demo.edu.cn',    '示例大学西区教师公寓5栋201室'),
(106, 106, '13700001106', 'hejing@demo.edu.cn',     '示例大学西区教师公寓5栋202室'),
(107, 107, '13700001107', 'liwenjing@demo.edu.cn',  '示例大学西区教师公寓6栋101室'),
(108, 108, '13700001108', 'chenzhiyuan@demo.edu.cn','示例大学西区教师公寓6栋102室'),
(109, 109, '13700001109', 'zhaoyaqin@demo.edu.cn',  '示例大学西区教师公寓6栋103室'),
(110, 110, '13700001110', 'sunhao@demo.edu.cn',     '示例大学西区教师公寓6栋104室'),
(111, 111, '13700001111', 'zhouli@demo.edu.cn',     '示例大学西区教师公寓6栋105室'),
(112, 112, '13700001112', 'machao@demo.edu.cn',     '示例大学西区教师公寓6栋106室'),
(113, 113, '13700001113', 'wangguoan@demo.edu.cn',  '示例大学西区教师公寓7栋201室'),
(114, 114, '13700001114', 'liuhuifang@demo.edu.cn', '示例大学西区教师公寓7栋202室'),
(115, 115, '13700001115', 'chenjianguo@demo.edu.cn','示例大学西区教师公寓7栋203室'),
(116, 116, '13700001116', 'zhangweiming@demo.edu.cn','示例大学西区教师公寓7栋204室'),
(117, 117, '13700001117', 'liguoqiang@demo.edu.cn', '示例大学西区教师公寓7栋205室'),
(118, 118, '13700001118', 'zhaolihua@demo.edu.cn',  '示例大学西区教师公寓8栋301室'),
(119, 119, '13700001119', 'sunwenhua@demo.edu.cn',  '示例大学西区教师公寓8栋302室'),
(120, 120, '13700001120', 'zhoumeiling@demo.edu.cn','示例大学西区教师公寓8栋303室');

COMMIT;

-- ============================================================
-- 9. 验证
-- ============================================================
-- 9.1 角色表（应只剩 1/2/3 + 117/118/119）
SELECT `id`, `code`, `name`, `level`, `is_auditor`, `scope_types`
FROM `roles` WHERE `deleted_at` IS NULL ORDER BY `id`;

-- 9.2 每个班是否恰好 1 名辅导员
SELECT cl.`id` AS class_id, cl.`name` AS class_name,
       COUNT(rs.`id`) AS counselor_cnt,
       GROUP_CONCAT(u.`name`) AS counselor_name
FROM `classes` cl
LEFT JOIN `role_scopes` rs ON rs.`scope_type` = 4 AND rs.`scope_id` = cl.`id` AND rs.`status` = 1
     AND rs.`role_id` = (SELECT `id` FROM `roles` WHERE `code` = 'counselor' AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1)
LEFT JOIN `users` u ON u.`id` = rs.`user_id`
WHERE cl.`deleted_at` IS NULL
GROUP BY cl.`id`, cl.`name`
ORDER BY cl.`id`;

-- 9.3 每个专业 1 名系主任、每个学院 1 名院长
SELECT '系主任' AS role_name, m.`id` AS scope_id, m.`name` AS scope_name,
       COUNT(rs.`id`) AS cnt, GROUP_CONCAT(u.`name`) AS user_names
FROM `majors` m
LEFT JOIN `role_scopes` rs ON rs.`scope_type` = 3 AND rs.`scope_id` = m.`id` AND rs.`status` = 1
     AND rs.`role_id` = (SELECT `id` FROM `roles` WHERE `code` = 'department_head' AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1)
LEFT JOIN `users` u ON u.`id` = rs.`user_id`
WHERE m.`deleted_at` IS NULL
GROUP BY m.`id`, m.`name`
UNION ALL
SELECT '院长', c.`id`, c.`name`, COUNT(rs.`id`), GROUP_CONCAT(u.`name`)
FROM `colleges` c
LEFT JOIN `role_scopes` rs ON rs.`scope_type` = 2 AND rs.`scope_id` = c.`id` AND rs.`status` = 1
     AND rs.`role_id` = (SELECT `id` FROM `roles` WHERE `code` = 'college_dean' AND `deleted_at` IS NULL ORDER BY `id` LIMIT 1)
LEFT JOIN `users` u ON u.`id` = rs.`user_id`
WHERE c.`deleted_at` IS NULL
GROUP BY c.`id`, c.`name`
ORDER BY role_name, scope_id;

-- 9.4 审批链完整性：每个学生 → 辅导员 → 系主任 → 院长
SELECT u.`id` AS student_id, u.`name` AS student, cl.`name` AS class_name, m.`name` AS major_name,
       col.`name` AS college_name,
       (SELECT u2.`name` FROM `role_scopes` rs2 JOIN `roles` r2 ON r2.`id`=rs2.`role_id` AND r2.`code`='counselor'
          JOIN `users` u2 ON u2.`id`=rs2.`user_id`
         WHERE rs2.`scope_type`=4 AND rs2.`scope_id`=cl.`id` AND rs2.`status`=1 LIMIT 1) AS counselor,
       (SELECT u3.`name` FROM `role_scopes` rs3 JOIN `roles` r3 ON r3.`id`=rs3.`role_id` AND r3.`code`='department_head'
          JOIN `users` u3 ON u3.`id`=rs3.`user_id`
         WHERE rs3.`scope_type`=3 AND rs3.`scope_id`=m.`id` AND rs3.`status`=1 LIMIT 1) AS dept_head,
       (SELECT u4.`name` FROM `role_scopes` rs4 JOIN `roles` r4 ON r4.`id`=rs4.`role_id` AND r4.`code`='college_dean'
          JOIN `users` u4 ON u4.`id`=rs4.`user_id`
         WHERE rs4.`scope_type`=2 AND rs4.`scope_id`=col.`id` AND rs4.`status`=1 LIMIT 1) AS dean
FROM `student_profiles` sp
JOIN `users` u ON u.`id` = sp.`user_id`
JOIN `classes` cl ON cl.`id` = sp.`class_id`
JOIN `majors` m ON m.`id` = cl.`major_id`
JOIN `colleges` col ON col.`id` = m.`college_id`
WHERE sp.`deleted_at` IS NULL AND u.`deleted_at` IS NULL
ORDER BY u.`id`;
