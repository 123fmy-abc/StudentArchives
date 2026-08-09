-- ============================================================
-- 种子数据：创建 5 条完整学生数据
-- 包含组织架构 + 用户 + 学生档案 + 联系信息
-- ============================================================

-- 1. 学校
INSERT INTO `schools` (`id`, `name`, `code`, `status`) VALUES
(1, '华中科技大学', 'HUST', 1);

-- 2. 学院
INSERT INTO `colleges` (`id`, `school_id`, `name`, `code`, `status`) VALUES
(1, 1, '计算机科学与技术学院', 'CS', 1);

-- 3. 专业
INSERT INTO `majors` (`id`, `college_id`, `name`, `code`, `status`) VALUES
(1, 1, '软件工程', 'SE', 1);

-- 4. 班级
INSERT INTO `classes` (`id`, `major_id`, `name`, `grade`, `student_count`, `status`) VALUES
(1, 1, '软件工程2024级1班', '2024级', 5, 1);

-- ============================================================
-- 5. 用户（学生）— 密码统一使用 "123456" (Bcrypt)
-- ============================================================
INSERT INTO `users` (`id`, `school_id`, `user_no`, `name`, `gender`, `birth_date`, `password`, `status`) VALUES
(1, 1, '202401001', '张三',   1, '2005-03-15', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(2, 1, '202401002', '李四',   1, '2005-07-22', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(3, 1, '202401003', '王五',   2, '2006-01-10', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(4, 1, '202401004', '赵六',   1, '2005-11-05', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1),
(5, 1, '202401005', '陈七',   2, '2005-09-18', '$2b$10$3R8lhzwuQJJZ3WscC/zdMODSTGK2/AhHGdxQbiC9OhZfmmUqIPbou', 1);

-- ============================================================
-- 6. 学生档案
-- ============================================================
-- political_status 存字典编码（对应 seed_dictionaries.sql 中 dict_type='political_status' 的 dict_code）
-- league_member=共青团员  masses=群众
INSERT INTO `student_profiles` (`id`, `user_id`, `class_id`, `political_status`, `student_status`, `degree_type`, `volunteer_hours`) VALUES
(1, 1, 1, 'league_member', 'current', 'undergraduate', 120.50),
(2, 2, 1, 'masses',        'current', 'undergraduate', 85.00),
(3, 3, 1, 'league_member', 'current', 'undergraduate', 200.00),
(4, 4, 1, 'league_member', 'current', 'undergraduate', 150.75),
(5, 5, 1, 'masses',        'current', 'undergraduate', 95.30);

-- ============================================================
-- 7. 联系信息（含指定 QQ 邮箱）
-- ============================================================
INSERT INTO `user_contact_infos` (`id`, `user_id`, `phone`, `email`, `address`) VALUES
(1, 1, '13800001001', '3227605507@qq.com',  '华中科技大学韵苑学生公寓1栋101室'),
(2, 2, '13800001002', '3596976474@qq.com',  '华中科技大学韵苑学生公寓1栋102室'),
(3, 3, '13800001003', 'wangwu@hust.edu.cn', '华中科技大学韵苑学生公寓1栋103室'),
(4, 4, '13800001004', 'zhaoliu@hust.edu.cn', '华中科技大学韵苑学生公寓1栋104室'),
(5, 5, '13800001005', 'chenqi@hust.edu.cn',  '华中科技大学韵苑学生公寓1栋105室');
