-- ============================================================
-- 种子数据：首页数据概览（GET /home/dashboard）
--
-- 为 5 名种子学生（seed_students.sql 中的张三~陈七，user_id=1~5）
-- 补齐首页概览所需的数据：学期成绩汇总、档案记录、奖项申报、站内消息。
-- 注：画像评分与数据完整度已随指标种子数据移除，首页对应模块将无测试数据。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql       （schools/colleges/majors/classes/users/user_contact_infos）
--   2. seed_semesters.sql      （semesters 1~5，semester_id=4 为当前学期 2025-2026-2）
--   3. seed_roles_permissions.sql （roles/user_roles）
--   4. seed_dictionaries.sql   （字典数据）
--
-- 数据口径（与《学生端接口文档》3.1 首页数据概览一致）：
--   - currentGpa/totalCredits/rankInClass/rankInMajor → semester_gpa_summaries（当前学期）
--   - applicationTotal/approvedCount/pendingCount/rejectedCount → archives（按 status 聚合）
--   - recentActivities → archives 最近提交
--   - unreadMessageCount → user_messages（is_read=0 且 is_archived=0）
-- ============================================================

-- ============================================================
-- 1. 学期成绩汇总（semester_gpa_summaries）
--    5 名学生 × 4 个已完成学期；当前学期（semester_id=4）用于首页 GPA 展示
--    class_id=1 / major_id=1 对应 seed_students.sql 的软件工程2024级1班
-- ============================================================
INSERT INTO `semester_gpa_summaries`
(`id`, `user_id`, `semester_id`, `class_id`, `major_id`, `course_count`, `total_credit`, `weighted_gpa`, `average_score`, `rank_in_class`, `rank_in_major`) VALUES
-- 张三（user_id=1），4 学期累计学分 86.5
(1,  1, 1, 1, 1, 8, 22.00, 3.75, 87.50, 3, 7),
(2,  1, 2, 1, 1, 8, 22.50, 3.78, 88.00, 3, 6),
(3,  1, 3, 1, 1, 7, 21.00, 3.80, 88.50, 2, 5),
(4,  1, 4, 1, 1, 7, 21.00, 3.82, 89.00, 2, 5),
-- 李四（user_id=2）
(5,  2, 1, 1, 1, 8, 22.00, 3.55, 85.00, 4, 8),
(6,  2, 2, 1, 1, 8, 22.50, 3.60, 85.50, 4, 9),
(7,  2, 3, 1, 1, 7, 21.00, 3.62, 86.00, 4, 9),
(8,  2, 4, 1, 1, 7, 21.00, 3.65, 86.50, 4, 8),
-- 王五（user_id=3）
(9,  3, 1, 1, 1, 8, 22.00, 3.85, 89.50, 1, 2),
(10, 3, 2, 1, 1, 8, 22.50, 3.88, 90.00, 1, 2),
(11, 3, 3, 1, 1, 7, 21.00, 3.90, 90.50, 1, 3),
(12, 3, 4, 1, 1, 7, 21.00, 3.91, 91.00, 1, 3),
-- 赵六（user_id=4）
(13, 4, 1, 1, 1, 8, 22.00, 3.70, 87.00, 2, 4),
(14, 4, 2, 1, 1, 8, 22.50, 3.73, 87.50, 2, 4),
(15, 4, 3, 1, 1, 7, 21.00, 3.75, 88.00, 3, 6),
(16, 4, 4, 1, 1, 7, 21.00, 3.76, 88.50, 3, 6),
-- 陈七（user_id=5）
(17, 5, 1, 1, 1, 8, 22.00, 3.30, 82.00, 5, 12),
(18, 5, 2, 1, 1, 8, 22.50, 3.35, 82.50, 5, 12),
(19, 5, 3, 1, 1, 7, 21.00, 3.38, 83.00, 5, 11),
(20, 5, 4, 1, 1, 7, 21.00, 3.42, 83.50, 5, 10);

-- ============================================================
-- 2. 档案记录（archives）— 首页概览的申报统计与最近动态
--    状态：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销（ApplyStatusEnum）
--    auditor_id 置 NULL（种子中暂无教师用户，审核人由后续教师数据补充）
--    说明：张三 12 条（8 通过 + 3 待审批 + 1 退回），其余学生各具代表性分布
-- ============================================================

-- ---- 张三（user_id=1）：12 条 ----
INSERT INTO `archives`
(`id`, `school_id`, `user_id`, `archive_type`, `title`, `semester_id`, `obtained_at`, `duplicate_check_status`, `status`, `rejected_reason`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`, `draft_saved_at`) VALUES
(1,  1, 1, 'competition',    '全国大学生数学建模竞赛（省级一等奖）',        4, '2025-11-20', 3, 2, NULL,      '2026-05-10 10:20:00', '2026-05-12 09:00:00', '2026-05-12 09:00:00', NULL, 1, 1, NULL),
(2,  1, 1, 'scholarship',    '国家励志奖学金',                             3, '2025-10-15', 3, 2, NULL,      '2025-10-20 14:00:00', '2025-10-25 10:00:00', '2025-10-25 10:00:00', NULL, 1, 1, NULL),
(3,  1, 1, 'social_practice', '暑期"三下乡"社会实践',                      3, '2025-08-20', 3, 2, NULL,      '2025-09-01 09:30:00', '2025-09-05 15:00:00', '2025-09-05 15:00:00', NULL, 1, 1, NULL),
(4,  1, 1, 'research',       '大学生创新创业训练计划立项（省级）',          3, '2025-12-01', 0, 1, NULL,      '2026-06-20 16:00:00', NULL, NULL, NULL, 1, 1, NULL),
(5,  1, 1, 'competition',    '蓝桥杯程序设计大赛（校级二等奖）',            4, '2026-04-15', 3, 2, NULL,      '2026-04-20 11:00:00', '2026-04-22 10:30:00', '2026-04-22 10:30:00', NULL, 1, 1, NULL),
(6,  1, 1, 'internship',     '武汉云途科技有限公司暑期实习',                2, '2025-08-31', 3, 2, NULL,      '2025-08-01 09:00:00', '2025-08-03 14:00:00', '2025-08-03 14:00:00', NULL, 1, 1, NULL),
(7,  1, 1, 'certificate',    '大学英语六级证书',                            2, '2025-06-14', 3, 2, NULL,      '2025-06-20 10:00:00', '2025-06-21 09:30:00', '2025-06-21 09:30:00', NULL, 1, 1, NULL),
(8,  1, 1, 'organization',   '院学生会宣传部干事',                          3, '2025-10-01', 3, 2, NULL,      '2025-10-08 15:00:00', '2025-10-10 11:00:00', '2025-10-10 11:00:00', NULL, 1, 1, NULL),
(9,  1, 1, 'training',       '软件工程师技能培训',                          3, '2025-11-10', 3, 2, NULL,      '2025-11-15 10:00:00', '2025-11-18 09:00:00', '2025-11-18 09:00:00', NULL, 1, 1, NULL),
(10, 1, 1, 'scholarship',    '校级优秀学生奖学金',                          4, '2026-06-01', 0, 1, NULL,      '2026-06-25 14:30:00', NULL, NULL, NULL, 1, 1, NULL),
(11, 1, 1, 'competition',    'ICPC 国际大学生程序设计竞赛亚洲区预选赛',      4, '2026-05-25', 0, 1, NULL,      '2026-06-28 14:30:00', NULL, NULL, NULL, 1, 1, NULL),
(12, 1, 1, 'social_practice', '社区疫情防控志愿服务',                       2, '2025-07-10', 3, 3, '证明材料不全，请补充盖章证明', '2025-07-15 09:00:00', '2025-07-16 10:00:00', NULL, '2025-07-16 10:00:00', 1, 1, NULL);

-- ---- 李四（user_id=2）：8 条 ----
INSERT INTO `archives`
(`id`, `school_id`, `user_id`, `archive_type`, `title`, `semester_id`, `obtained_at`, `duplicate_check_status`, `status`, `rejected_reason`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`, `draft_saved_at`) VALUES
(13, 1, 2, 'competition',    '全国大学生英语竞赛（校级三等奖）',            4, '2026-04-18', 3, 2, NULL,   '2026-04-22 10:00:00', '2026-04-24 09:00:00', '2026-04-24 09:00:00', NULL, 1, 1, NULL),
(14, 1, 2, 'scholarship',    '国家奖学金',                                 4, '2026-06-01', 0, 1, NULL,   '2026-06-22 15:00:00', NULL, NULL, NULL, 1, 1, NULL),
(15, 1, 2, 'social_practice', '养老院志愿服务活动',                         3, '2025-12-05', 3, 2, NULL,   '2025-12-10 09:30:00', '2025-12-12 10:00:00', '2025-12-12 10:00:00', NULL, 1, 1, NULL),
(16, 1, 2, 'certificate',    '计算机二级证书',                              3, '2025-09-20', 3, 2, NULL,   '2025-09-25 14:00:00', '2025-09-28 09:00:00', '2025-09-28 09:00:00', NULL, 1, 1, NULL),
(17, 1, 2, 'research',       '学术论文（合作作者，普刊）',                  4, '2026-03-15', 3, 2, NULL,   '2026-03-20 10:00:00', '2026-03-23 11:00:00', '2026-03-23 11:00:00', NULL, 1, 1, NULL),
(18, 1, 2, 'internship',     '社区服务中心实习',                            4, NULL,        0, 0, NULL,   NULL, NULL, NULL, NULL, 1, 0, '2026-06-30 20:00:00'),
(19, 1, 2, 'organization',   '班级体育委员',                                3, '2025-10-01', 3, 2, NULL,   '2025-10-05 09:00:00', '2025-10-07 14:00:00', '2025-10-07 14:00:00', NULL, 1, 1, NULL),
(20, 1, 2, 'competition',    '全国大学生数学竞赛',                          3, '2025-12-10', 3, 3, '证书照片模糊，请重新上传', '2025-12-15 10:00:00', '2025-12-16 09:30:00', NULL, '2025-12-16 09:30:00', 1, 1, NULL);

-- ---- 王五（user_id=3）：9 条 ----
INSERT INTO `archives`
(`id`, `school_id`, `user_id`, `archive_type`, `title`, `semester_id`, `obtained_at`, `duplicate_check_status`, `status`, `rejected_reason`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`, `draft_saved_at`) VALUES
(21, 1, 3, 'competition',    '挑战杯大学生课外学术科技作品竞赛（省级一等奖）', 4, '2026-05-10', 3, 2, NULL, '2026-05-15 10:00:00', '2026-05-18 09:00:00', '2026-05-18 09:00:00', NULL, 1, 1, NULL),
(22, 1, 3, 'scholarship',    '校级一等奖学金',                               4, '2026-06-01', 3, 2, NULL, '2026-06-08 11:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL, 1, 1, NULL),
(23, 1, 3, 'research',       '大学生创新训练计划（国家级立项）',              3, '2025-11-15', 3, 2, NULL, '2025-11-20 09:00:00', '2025-11-22 15:00:00', '2025-11-22 15:00:00', NULL, 1, 1, NULL),
(24, 1, 3, 'research',       '发表 SCI 论文一篇',                            4, '2026-03-01', 3, 2, NULL, '2026-03-05 14:00:00', '2026-03-08 09:00:00', '2026-03-08 09:00:00', NULL, 1, 1, NULL),
(25, 1, 3, 'social_practice', '乡村振兴调研社会实践',                         3, '2025-08-25', 3, 2, NULL, '2025-09-02 10:00:00', '2025-09-04 11:00:00', '2025-09-04 11:00:00', NULL, 1, 1, NULL),
(26, 1, 3, 'internship',     '华为武汉研究所暑期实习',                        4, '2026-08-31', 0, 1, NULL, '2026-06-30 09:00:00', NULL, NULL, NULL, 1, 1, NULL),
(27, 1, 3, 'organization',   '校学生会主席团成员',                            3, '2025-10-01', 3, 2, NULL, '2025-10-06 09:30:00', '2025-10-09 10:00:00', '2025-10-09 10:00:00', NULL, 1, 1, NULL),
(28, 1, 3, 'training',       '区块链开发认证',                                3, '2025-12-01', 3, 2, NULL, '2025-12-05 14:00:00', '2025-12-08 09:00:00', '2025-12-08 09:00:00', NULL, 1, 1, NULL),
(29, 1, 3, 'competition',    '中国国际大学生创新大赛',                        4, '2026-05-30', 3, 3, '提交材料未盖章，退回补充', '2026-06-02 10:00:00', '2026-06-03 09:30:00', NULL, '2026-06-03 09:30:00', 1, 1, NULL);

-- ---- 赵六（user_id=4）：6 条 ----
INSERT INTO `archives`
(`id`, `school_id`, `user_id`, `archive_type`, `title`, `semester_id`, `obtained_at`, `duplicate_check_status`, `status`, `rejected_reason`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`, `draft_saved_at`) VALUES
(30, 1, 4, 'competition',    '蓝桥杯程序设计大赛（省级三等奖）',            4, '2026-04-12', 3, 2, NULL, '2026-04-18 10:00:00', '2026-04-20 09:00:00', '2026-04-20 09:00:00', NULL, 1, 1, NULL),
(31, 1, 4, 'scholarship',    '校级二等奖学金',                             4, '2026-06-01', 0, 1, NULL, '2026-06-20 14:00:00', NULL, NULL, NULL, 1, 1, NULL),
(32, 1, 4, 'social_practice', '博物馆志愿讲解',                             3, '2025-11-20', 3, 2, NULL, '2025-11-25 09:00:00', '2025-11-27 10:00:00', '2025-11-27 10:00:00', NULL, 1, 1, NULL),
(33, 1, 4, 'certificate',    '普通话二级甲等证书',                         3, '2025-09-10', 3, 2, NULL, '2025-09-15 15:00:00', '2025-09-18 09:00:00', '2025-09-18 09:00:00', NULL, 1, 1, NULL),
(34, 1, 4, 'internship',     '软件外包公司实习',                           4, NULL,        0, 0, NULL, NULL, NULL, NULL, NULL, 1, 0, '2026-07-01 21:00:00'),
(35, 1, 4, 'research',       '参与教师横向课题',                           4, '2026-04-30', 3, 2, NULL, '2026-05-05 10:00:00', '2026-05-08 09:00:00', '2026-05-08 09:00:00', NULL, 1, 1, NULL);

-- ---- 陈七（user_id=5）：5 条 ----
INSERT INTO `archives`
(`id`, `school_id`, `user_id`, `archive_type`, `title`, `semester_id`, `obtained_at`, `duplicate_check_status`, `status`, `rejected_reason`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`, `draft_saved_at`) VALUES
(36, 1, 5, 'competition',    '大学生电子设计竞赛（校级二等奖）',            4, '2026-04-20', 3, 2, NULL, '2026-04-25 10:00:00', '2026-04-28 09:00:00', '2026-04-28 09:00:00', NULL, 1, 1, NULL),
(37, 1, 5, 'scholarship',    '国家励志奖学金',                             4, '2026-06-01', 0, 1, NULL, '2026-06-18 14:00:00', NULL, NULL, NULL, 1, 1, NULL),
(38, 1, 5, 'social_practice', '社区环保公益活动',                           3, '2025-10-30', 3, 2, NULL, '2025-11-02 09:00:00', '2025-11-05 10:00:00', '2025-11-05 10:00:00', NULL, 1, 1, NULL),
(39, 1, 5, 'organization',   '志愿者协会干事',                             3, '2025-10-01', 3, 2, NULL, '2025-10-08 15:00:00', '2025-10-10 09:00:00', '2025-10-10 09:00:00', NULL, 1, 1, NULL),
(40, 1, 5, 'research',       '参与大创项目',                               3, '2025-12-15', 3, 3, '项目结题材料不完整', '2025-12-20 10:00:00', '2025-12-22 09:30:00', NULL, '2025-12-22 09:30:00', 1, 1, NULL);

-- ============================================================
-- 3. 奖项申报（award_applications）— 供奖项总览统计（GET /awards/overview）
--    award_type 编码：competition_star / research_star / innovation_star
-- ============================================================
INSERT INTO `award_applications`
(`id`, `school_id`, `user_id`, `award_type`, `title`, `semester_id`, `certificate_no`, `issuing_unit`, `valid_until`, `participant_role`, `status`, `submitted_at`, `audited_at`, `passed_at`, `returned_at`, `current_version`, `submit_count`) VALUES
(1, 1, 1, 'competition_star', '全国大学生数学建模竞赛',         4, 'MCM-2025-00128', '中国工业与应用数学学会', '2026-12-31', '队长',   2, '2026-06-10 10:00:00', '2026-06-12 09:00:00', '2026-06-12 09:00:00', NULL, 1, 1),
(2, 1, 1, 'innovation_star',  '大学生创新创业大赛',             4, NULL,             '教育部',                NULL,          '成员',   1, '2026-06-28 14:30:00', NULL, NULL, NULL, 1, 1),
(3, 1, 2, 'competition_star', '全国大学生英语竞赛',             3, 'NECCS-2025-008',  '高等学校大学外语教学指导委员会', NULL, '参赛者', 2, '2025-12-01 09:00:00', '2025-12-03 10:00:00', '2025-12-03 10:00:00', NULL, 1, 1),
(4, 1, 3, 'competition_star', '挑战杯大学生课外学术科技作品竞赛', 4, 'TZB-2026-00051',  '共青团中央',           NULL,          '队长',   2, '2026-05-20 10:00:00', '2026-05-22 09:30:00', '2026-05-22 09:30:00', NULL, 1, 1),
(5, 1, 3, 'research_star',    '优秀科研之星',                   4, NULL,             '校科研处',             NULL,          '成员',   1, '2026-06-25 15:00:00', NULL, NULL, NULL, 1, 1),
(6, 1, 4, 'competition_star', '蓝桥杯程序设计大赛',             4, 'LBQ-2026-00336',  '工业和信息化部人才交流中心', NULL,   '参赛者', 1, '2026-06-20 10:00:00', NULL, NULL, NULL, 1, 1),
(7, 1, 5, 'competition_star', '大学生电子设计竞赛',             4, 'EDC-2026-00115',  '教育部高等教育司',       NULL,          '队长',   2, '2026-05-10 09:00:00', '2026-05-12 10:00:00', '2026-05-12 10:00:00', NULL, 1, 1);

-- ============================================================
-- 3.1 奖项报名扩展表（与 3 中 award_applications 一一对应）
--     竞赛之星（competition_star）→ award_competition_stars
--     双创之星（innovation_star） → award_innovation_stars
--     科研之星（research_star）   → award_research_stars + 科研项目子表
--     简历导出（award_competition_stars 等）依赖此明细，缺明细时基表字段无法补齐
-- ============================================================
-- 3.1.1 竞赛之星（对应报名 id 1、3、4、6、7）
INSERT INTO `award_competition_stars`
(`id`, `application_id`, `competition_name`, `participated_at`, `competition_level`, `award_level`) VALUES
(1, 1, '全国大学生数学建模竞赛', '2025-09-12', '国家级', '一等奖'),
(2, 3, '全国大学生英语竞赛',     '2025-11-16', '国家级', '二等奖'),
(3, 4, '挑战杯大学生课外学术科技作品竞赛', '2026-04-18', '国家级', '一等奖'),
(4, 6, '蓝桥杯程序设计大赛',     '2026-03-14', '国家级', '三等奖'),
(5, 7, '大学生电子设计竞赛',     '2026-04-25', '国家级', '二等奖');

-- 3.1.2 双创之星（对应报名 id 2）
INSERT INTO `award_innovation_stars`
(`id`, `application_id`, `company_name`, `industry_type`, `applicant_rank`, `registered_at`) VALUES
(1, 2, '星辰科创有限责任公司', '软件和信息技术服务业', '联合创始人', '2025-10-08');

-- 3.1.3 科研之星（对应报名 id 5，主要成果类型=project）
INSERT INTO `award_research_stars`
(`id`, `application_id`, `primary_category`) VALUES
(1, 5, 'project');

-- 3.1.4 科研之星子表：科研项目（关联 award_research_stars.id=1）
INSERT INTO `award_research_projects`
(`id`, `research_star_id`, `project_name`, `project_level`, `rank_total`, `established_at`) VALUES
(1, 1, '基于大数据的智慧校园学习行为分析平台', '校级', '1/6', '2025-09-01');

-- ============================================================
-- 3.2 奖项类型配置（award_type_configs）— 奖项报名模块的类型配置
--     编码与 3 中 award_applications.award_type 对应，供奖项申报/评选说明使用
-- ============================================================
INSERT INTO `award_type_configs`
(`id`, `award_type`, `type_name`, `evaluate_desc`, `apply_desc`, `icon`, `sort`, `status`) VALUES
(1, 'competition_star', '竞赛之星',
 '竞赛之星用于表彰在学科竞赛中取得优异成绩的学生。申报需提供竞赛名称、竞赛级别（国家级/省部级/校级等）、获奖级别，并上传获奖证书或参赛证明。',
 '1. 每人每学期限申报一项；\n2. 佐证材料须包含获奖证书或官方公示截图；\n3. 团队获奖须注明本人排名。',
 'trophy', 1, 1),
(2, 'research_star', '科研之星',
 '科研之星用于表彰在科研活动中表现突出的学生，主要成果类型包括科研项目、软件著作权、发表论文三类。',
 '1. 主要成果类型为科研项目/软件著作权/发表论文三选一；\n2. 须提供立项、获批或发表证明材料；\n3. 团队成果须注明本人排名。',
 'flask', 2, 1),
(3, 'innovation_star', '双创之星',
 '双创之星用于表彰在创新创业实践中表现突出的学生，申报需提供创业公司名称、行业类型、申报人排名及注册时间。',
 '1. 公司须为在校期间注册；\n2. 须提供营业执照等佐证材料；\n3. 须注明申报人排名。',
 'lightbulb', 3, 1);

-- ============================================================
-- 4. 站内消息（user_messages）— 首页未读消息数
--    category：system_notice / audit_remind / dynamic_remind（对应 seed_dictionaries.sql）
--    sender_type：1=系统 2=人工 3=自动触发；sender_id NULL（种子中暂无教师用户）
--    张三 5 条未读，李四 3 条，王五 2 条，赵六 2 条，陈七 1 条
-- ============================================================
INSERT INTO `user_messages`
(`id`, `user_id`, `sender_id`, `sender_type`, `category`, `title`, `content`, `related_type`, `related_id`, `send_channel`, `is_read`, `read_at`, `is_archived`, `is_important`, `jump_url`) VALUES
(1,  1, NULL, 3, 'audit_remind',   '学科竞赛申报已提交', '你的"全国大学生数学建模竞赛"申报已提交，请等待审核。', 'archive', 1, 'push', 0, NULL, 0, 0, '/applications/competition'),
(2,  1, NULL, 3, 'audit_remind',   '档案审核通过',       '你的"国家励志奖学金"档案已审核通过。',                 'archive', 2, 'push', 1, '2026-05-12 10:00:00', 0, 0, '/archive/2'),
(3,  1, NULL, 1, 'system_notice',  '系统更新通知',       '学生成长档案系统已升级至 V5.0，新增指标树与自动评分能力。', NULL, NULL, 'push', 0, NULL, 0, 0, '/announcements/1'),
(4,  1, NULL, 3, 'dynamic_remind', '成长计划教师反馈',   '导师对你的成长计划给出了反馈，请及时查看。',           'career_plan', 1, 'push', 0, NULL, 0, 0, '/career-plan'),
(5,  1, NULL, 3, 'audit_remind',   '申报被退回',         '你的"社区疫情防控志愿服务"申报被退回：证明材料不全，请补充盖章证明。', 'archive', 12, 'push', 1, '2025-07-17 09:00:00', 0, 0, '/archive/12'),
(6,  1, NULL, 1, 'system_notice',  '新学期开学通知',     '2025-2026-2 学期选课已开始，请按时完成选课。',        NULL, NULL, 'push', 0, NULL, 0, 0, '/announcements/2'),
(7,  1, NULL, 3, 'dynamic_remind', '志愿时长已更新',     '你的志愿时长已更新为 120.5 小时。',                   NULL, NULL, 'push', 0, NULL, 0, 0, NULL),
(8,  2, NULL, 3, 'audit_remind',   '档案审核通过',       '你的"全国大学生英语竞赛"档案已审核通过。',             'archive', 13, 'push', 1, '2026-04-25 09:00:00', 0, 0, '/archive/13'),
(9,  2, NULL, 1, 'system_notice',  '奖学金申报开始',     '国家奖学金申报已开启，请在截止日期前提交。',           NULL, NULL, 'push', 0, NULL, 0, 0, '/applications/scholarship'),
(10, 2, NULL, 3, 'audit_remind',   '申报被退回',         '你的"全国大学生数学竞赛"申报被退回：证书照片模糊，请重新上传。', 'archive', 20, 'push', 1, '2025-12-17 09:00:00', 0, 0, '/archive/20'),
(11, 2, NULL, 3, 'dynamic_remind', '成长计划进度提醒',   '本学期成长计划已完成 60%。',                            NULL, NULL, 'push', 0, NULL, 0, 0, '/career-plan'),
(12, 2, NULL, 1, 'system_notice',  '成绩已导入',         '2025-2026-2 学期成绩已由教务处导入。',                 NULL, NULL, 'push', 0, NULL, 0, 0, NULL),
(13, 3, NULL, 3, 'audit_remind',   '档案审核通过',       '你的"挑战杯"档案已审核通过。',                         'archive', 21, 'push', 1, '2026-05-19 09:00:00', 0, 0, '/archive/21'),
(14, 3, NULL, 3, 'dynamic_remind', '科研之星评选',       '你已进入"科研之星"评选名单，请完善申报材料。',         'award', 5, 'push', 1, '2026-06-01 09:00:00', 0, 0, '/awards/research-star'),
(15, 3, NULL, 3, 'dynamic_remind', '志愿时长已更新',     '你的志愿时长已更新为 200 小时。',                      NULL, NULL, 'push', 1, '2026-06-01 09:00:00', 0, 0, NULL),
(16, 3, NULL, 1, 'system_notice',  '系统维护通知',       '系统将于本周末 22:00-06:00 进行维护。',               NULL, NULL, 'push', 0, NULL, 0, 0, '/announcements/3'),
(17, 4, NULL, 1, 'system_notice',  '选课通知',           '2025-2026-2 学期选课已开放，请及时选课。',             NULL, NULL, 'push', 0, NULL, 0, 0, '/announcements/4'),
(18, 4, NULL, 3, 'dynamic_remind', '成长计划反馈',       '导师已查看你的成长计划，暂无修改意见。',               'career_plan', 2, 'push', 0, NULL, 0, 0, '/career-plan'),
(19, 4, NULL, 3, 'audit_remind',   '申报被退回',         '你的"参与大创项目"申报被退回：项目结题材料不完整。',   'archive', 40, 'push', 1, '2025-12-23 09:00:00', 0, 0, '/archive/40'),
(20, 5, NULL, 3, 'audit_remind',   '档案审核通过',       '你的"大学生电子设计竞赛"档案已审核通过。',             'archive', 36, 'push', 1, '2026-04-29 09:00:00', 0, 0, '/archive/36'),
(21, 5, NULL, 1, 'system_notice',  '奖学金申报提醒',     '国家励志奖学金申报即将截止，请及时提交。',             NULL, NULL, 'push', 0, NULL, 0, 0, '/applications/scholarship');
