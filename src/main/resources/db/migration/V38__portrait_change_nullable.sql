-- ============================================================
-- V38：portrait_evaluation_scores.change 放开为可空（无对比学期 → NULL）
--
-- 背景（2026-10-08 问题清单 §1.5）：
--   /home/dashboard 的 indicators[].trend 与 /profile/scores 的 list[].change
--   在**没有对比学期**时返回 "+0"，读起来是「与上期持平」，实际含义是「没有上期可比较」。
--   根因：AdminScoreService 在 comparedSemesterId 为空时用 ZERO 兜底（原 :821），
--   落库成 0，再被 formatTrend 格式化为 "+0"。
--
--   代码侧已改为写 null（不再以 ZERO 兜底）。但 V7 建表时该列为
--     `change` DECIMAL(5,2) NOT NULL DEFAULT 0
--   写入 NULL 会被数据库拒绝（ERROR 1048），故需先放开可空约束。
--
-- 语义（与 §1.4 的 hasPrevious 开关同源）：
--   change 为 NULL ⇔ compared_semester_id 为 NULL ⇔ 无上一学期可比；
--   change 有值（含 0.00）⇔ 与上一学期相比确实持平。
--   JSON 侧由 @JsonInclude(NON_NULL) 决定是否省略该字段。
--
-- 说明：DEFAULT 一并改为 NULL —— 保留 DEFAULT 0 会让"未显式赋值"的插入
--   静默变成 0.00，重新引入本 bug。应用层每次写入都会显式赋值，无兼容风险。
--
-- 存量数据修复不在此迁移内：清掉历史的脏 0 属数据修复，见根目录
--   repair_2026-10-08_issues.sql（幂等、可重复执行），不在所有环境自动跑。
-- ============================================================

ALTER TABLE `portrait_evaluation_scores`
    MODIFY COLUMN `change` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '较上阶段变化；无对比学期时为 NULL';
