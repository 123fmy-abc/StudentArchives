-- ============================================================
-- 种子数据：消息中心模块（五、消息中心模块 /messages）
--
-- 为 5 名种子学生（seed_students.sql 中的张三~陈七，user_id=1~5）
-- 补齐消息中心 5.1~5.7 所需数据：
--   - user_messages          → 消息列表（分类/已读/归档/关键词筛选）
--   - notification_settings  → 消息通知设置（5.6 查询 / 5.7 更新）
--
-- ⚠️ ID 从 100 起：团队共享库 user_messages 已有数据（id 1~36），
--    使用高位显式 ID 避免主键冲突；执行前请先确认无冲突。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql         （users 1~5，张三~陈七）
--   2. seed_semesters.sql
--   3. seed_roles_permissions.sql
--   4. seed_admins.sql           （users 6~7）
--   5. seed_teachers.sql         （users 8~10，王建国/刘秀英/孙志强）
--   6. seed_dictionaries.sql     （message_category 字典编码）
--
-- 数据口径（与《学生端接口文档》V5.7 一致）：
--   - sender_type：1=系统 2=人工 3=自动触发；系统消息 sender_id=NULL
--   - category：system_notice/audit_remind/dynamic_remind/private_message
--   - is_read：0=未读 1=已读；is_archived：0=未归档 1=已归档
--   - is_important：1=重要消息（归档前需二次确认）
--   - read_all：张三（user_id=1）含未读消息，可验证 5.3 批量已读
--   - 已归档消息：张三 1 条、王五 1 条，可验证 5.4/5.5 归档与取消归档
--   - 私信：张三收到教师孙志强（user_id=10）私信，验证 senderName 关联
--
-- 常用测试消息 ID：
--   100 奖学金申报已通过（未读/重要）  101 学期档案申报开始（已读/含截止时间）
--   103 成长时间轴已更新（未读）       104 社会实践申报已退回（已读）
--   105 职业规划修改建议（私信/已归档）110 奖学金申报待审核（未读/重要）
--   111 论文修改意见（私信/已归档）
-- ============================================================

-- ============================================================
-- 1. 用户消息（user_messages）
--    张三（user_id=1）5 条与文档示例贴近；其余学生各 3 条
-- ============================================================
INSERT INTO `user_messages` (`id`, `user_id`, `sender_id`, `sender_type`, `template_id`, `category`, `title`, `content`, `related_type`, `related_id`, `send_channel`, `is_read`, `read_at`, `is_archived`, `archived_at`, `is_important`, `deadline`, `jump_url`) VALUES
-- ---- 张三（user_id=1，未读 2 条 / 已读 3 条 / 已归档 1 条）----
(100, 1, NULL, 1, NULL, 'audit_remind',    '奖学金申报已通过',     '您的国家奖学金申请已通过审核。',            'award_applications', 100, 'push', 0, NULL, 0, NULL, 1, NULL,                '/applications/award/100'),
(101, 1, NULL, 1, NULL, 'system_notice',   '学期档案申报开始',     '2025-2026学年第二学期档案申报已开放。',     NULL,                NULL, 'push', 1, '2026-07-04 10:00:00', 0, NULL, 0, '2026-07-10 23:59:59', NULL),
(102, 1, NULL, 1, NULL, 'dynamic_remind',  '成长时间轴已更新',     '您本学期的成长时间轴新增了 2 条记录。',     'growth_timelines',    5, 'push', 0, NULL, 0, NULL, 0, NULL,                '/growth-timeline'),
(103, 1, NULL, 1, NULL, 'audit_remind',    '社会实践申报已退回',   '您的暑期社会实践申报已被退回，请修改后重新提交。', 'archive_social_practices', 8, 'push', 1, '2026-07-05 09:30:00', 0, NULL, 0, NULL, '/applications/practice/8'),
(104, 1, 10,   2, NULL, 'private_message', '职业规划修改建议',     '孙老师：建议在大二规划中补充科研项目目标。', 'career_plans',        3, 'push', 1, '2026-07-06 15:20:00', 1, '2026-07-07 10:00:00', 0, NULL, '/profile/career-plans/3'),
-- ---- 李四（user_id=2，未读 2 条 / 已读 1 条）----
(105, 2, NULL, 1, NULL, 'system_notice',   '系统升级公告',         '系统将于本周六 02:00-04:00 进行升级维护。',  NULL,                NULL, 'push', 0, NULL, 0, NULL, 0, '2026-07-12 04:00:00', NULL),
(106, 2, NULL, 3, NULL, 'audit_remind',    '竞赛申报已通过',       '您的全国大学生英语竞赛申报已通过审核。',    'award_applications', 102, 'push', 1, '2026-07-03 14:00:00', 0, NULL, 0, NULL, '/applications/award/102'),
(107, 2, NULL, 1, NULL, 'dynamic_remind',  '数据完整度提示',       '您的画像数据完整度为 45%，建议补充科研经历。', NULL,              NULL, 'push', 0, NULL, 0, NULL, 0, NULL, '/profile/data-completeness'),
-- ---- 王五（user_id=3，已归档 1 条）----
(108, 3, NULL, 1, NULL, 'system_notice',   '新学期选课通知',       '2026-2027学年第一学期选课将于下周开放。',    NULL,                NULL, 'push', 1, '2026-07-02 08:00:00', 0, NULL, 0, '2026-07-20 23:59:59', NULL),
(109, 3, NULL, 1, NULL, 'audit_remind',    '奖学金申报待审核',     '您的国家励志奖学金申报正在审批中。',         'award_applications', 103, 'push', 0, NULL, 0, NULL, 1, NULL, '/applications/award/103'),
(110, 3, 9,    2, NULL, 'private_message', '论文修改意见',         '刘老师：请按标注意见修改论文后重新提交。',   'archives',           20, 'push', 1, '2026-07-05 11:00:00', 1, '2026-07-06 09:00:00', 0, NULL, '/applications/research/20'),
-- ---- 赵六（user_id=4，全部未读）----
(111, 4, NULL, 1, NULL, 'system_notice',   '毕业去向登记提醒',     '请于本月内完成毕业去向登记。',               NULL,                NULL, 'push', 0, NULL, 0, NULL, 1, '2026-07-31 23:59:59', NULL),
(112, 4, NULL, 3, NULL, 'dynamic_remind',  '志愿时长更新',         '您本月的志愿服务时长已更新为 150.75 小时。', NULL,                NULL, 'push', 0, NULL, 0, NULL, 0, NULL, '/profile/info'),
(113, 4, NULL, 1, NULL, 'audit_remind',    '学科竞赛申报已退回',   '您的蓝桥杯竞赛申报材料不完整，请补充后重新提交。', 'award_applications', 104, 'push', 0, NULL, 0, NULL, 0, NULL, '/applications/award/104'),
-- ---- 陈七（user_id=5，已读 2 条 / 未读 1 条，含邮件渠道）----
(114, 5, NULL, 1, NULL, 'system_notice',   '成绩单开放查询',       '本学期期末成绩已开放查询。',                  NULL,                NULL, 'push', 1, '2026-07-04 16:00:00', 0, NULL, 0, NULL, '/profile/scores'),
(115, 5, NULL, 3, NULL, 'dynamic_remind',  '短板改进建议',         '系统为您生成了新的短板改进建议，请查看。',   'weakness_analyses',   6, 'push', 0, NULL, 0, NULL, 0, NULL, '/profile/info'),
(116, 5, 8,    2, NULL, 'private_message', '实习证明审核通知',     '王老师：您的实习证明已审核通过。',            'archives',           25, 'email', 1, '2026-07-06 17:30:00', 0, NULL, 0, NULL, '/applications/practice/25');

-- ============================================================
-- 2. 消息通知设置（notification_settings）
--    张三已配置 2 类（自定义）；其余学生使用系统默认（可验证 5.6 默认值兜底）
-- ============================================================
INSERT INTO `notification_settings` (`id`, `user_id`, `category`, `email_enabled`, `sms_enabled`, `push_enabled`) VALUES
(100, 1, 'audit_remind',   1, 0, 1),
(101, 1, 'system_notice',  1, 1, 1),
(102, 2, 'audit_remind',   0, 0, 1),
(103, 2, 'private_message', 1, 1, 1),
(104, 3, 'system_notice',  1, 0, 0),
(105, 4, 'dynamic_remind', 0, 1, 1);
