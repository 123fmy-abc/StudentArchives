-- ============================================================
-- 种子数据：AI 改进建议（improvement_suggestions）
-- 供接口测试：POST /profile/career-plans/ai-add（AI建议一键添加为计划，学生端 4.15）
--
-- 用途：为 5 名种子学生（seed_students.sql 中的张三~陈七，user_id=1~5）
--       各补齐 1~2 条改进建议，使 ai-add 接口可对每条建议一键生成职业规划
--       （career_plans.source=2、ai_suggestion_id 记录来源，goal/action.source=2）。
--
-- 前提：已依次执行以下种子文件
--   1. seed_students.sql      （schools id=1 / users id=1~5）
--   2. seed_semesters.sql     （semesters 1~5，当前学期 id=4）
--   3. seed_profile.sql       （weakness_analyses id=1~7，归属 user_id=1~5）
--   4. seed_teachers.sql      （users 8~10，weakness 4 教师建议来源 teacher_id=2 刘秀英）
--
-- 数据口径（与 ProfileCareerPlanService.aiAddPlan 逻辑一致）：
--   - weakness_id 关联现有 weakness_analyses.id：接口据此校验建议归属当前登录学生
--     （经 weakness_id → weakness_analyses.user_id），非本人调用返回 403 无访问权限。
--   - suggestion_content 为多行文本：接口按换行拆分成行（非空、上限 10 行），
--     每行生成 1 条 career_goals + 1 条 career_actions（goalTitle/actionTitle=该行），
--     规划的默认标题取 content 第一行。建议多行便于一次生成多条目标/行动。
--   - source：1=AI生成 2=教师建议；weakness_analyses.id=4（王五·社会工作）为教师建议
--     （source=2, teacher_id=2），对应建议一并标为教师建议，保持来源链路一致。
--   - 最后一条 weakness_id=NULL（不关联短板）：测试接口「weaknessId 为空时跳过
--     归属校验」的分支。
--
-- 说明：
--   - is_deleted_null 为生成列，无需插入；created_at/updated_at 走默认值。
--   - 导入请使用 utf8mb4 字符集，避免中文乱码：
--       mysql --default-character-set=utf8mb4 -h 8.137.191.26 -u StuArch -p student_archives < seed_improvement_suggestions.sql
-- ============================================================

INSERT INTO `improvement_suggestions`
(`id`, `weakness_id`, `suggestion_type`, `related_goal_id`, `suggestion_content`, `source`, `teacher_id`, `is_implemented`) VALUES
-- ---- 张三（user_id=1）----
-- weakness 1=科研创新：多行 → 5 条目标/行动
(1,  1, '科研提升', NULL, '加强科研训练，提升论文写作与发表能力
加入导师课题组，承担一项子课题研究工作
每周阅读 2 篇高水平论文，完成文献综述整理
本学期完成 1 篇学术论文初稿并投稿
参加 1 次学术会议，完成组内汇报', 1, NULL, 0),
-- weakness 2=竞赛实践：多行 → 4 条目标/行动
(2,  2, '竞赛提升', NULL, '系统备战学科竞赛，冲击省级以上奖项
加入 ACM 集训队，每周参加 2 次集中训练
报名 2026 蓝桥杯省级赛并完成备赛刷题
参加 2 场 Codeforces 虚拟赛并复盘总结', 1, NULL, 0),
-- ---- 李四（user_id=2）----
-- weakness 3=科研创新：多行 → 4 条目标/行动
(3,  3, '科研提升', NULL, '提升科研产出，补齐论文发表短板
参与导师横向课题，负责数据采集与建模
每月精读 2 篇英文论文并输出笔记
完成 1 篇课程论文并投稿校内期刊', 1, NULL, 0),
-- ---- 王五（user_id=3）----
-- weakness 4=社会工作（教师建议 source=2）：多行 → 4 条目标/行动
(4,  4, '社会能力提升', NULL, '提升社会工作参与度，积累志愿服务时长
担任班级公益委员，每月组织 1 次志愿活动
本学年完成 30 小时志愿服务并记录时长
参与校团委"三下乡"实践活动并完成申报', 2, 2, 0),
-- ---- 赵六（user_id=4）----
-- weakness 5=科研创新：多行 → 4 条目标/行动
(5,  5, '科研提升', NULL, '深化科研参与，提升科研能力
加入学院创新实验室，完成 1 项综合项目
学习 LaTeX，规范科研文档写作
申请 1 项大学生创新创业训练计划项目', 1, NULL, 0),
-- ---- 陈七（user_id=5）----
-- weakness 6=学业成绩：多行 → 5 条目标/行动
(6,  6, '学业提升', NULL, '提升学业成绩，改善绩点与外语水平
制定每周学习计划并坚持每日复盘
本学期通过大学英语六级（CET-6）
重修《高等数学》，期末目标 85 分以上
参加学业帮扶小组，每周集中自习 3 次', 1, NULL, 0),
-- weakness 7=竞赛实践：多行 → 4 条目标/行动
(7,  7, '竞赛提升', NULL, '提升竞赛水平，冲击更高等级奖项
参加大学生电子设计竞赛培训营
完成 3 套历年真题并整理解题报告
报名 2026 校级竞赛并冲击省赛', 1, NULL, 0),
-- ---- 通用建议（weakness_id=NULL）----
-- 不关联短板：测试 aiAddPlan「weaknessId 为空时跳过归属校验」的分支
(8,  NULL, '综合提升', NULL, '全面提升综合素养，为未来发展打基础
每周参加 1 次校园讲座并整理笔记
考取 1 项行业认证证书
坚持运动，每周锻炼 3 次', 1, NULL, 0);
