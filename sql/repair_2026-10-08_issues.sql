-- ============================================================
-- 修复脚本：2026-10-08 后端需处理问题清单（§1.1 / §1.2 / §1.5）
-- ============================================================
-- 背景：前端（ry）在 2026-10-08 对账后给出
--   `2026-10-08-后端需处理问题清单.md`，其中三条属"数据问题，非代码 bug"：
--     §1.1 🔴 P0 同一指标「志愿时长达标」挂在两个维度下重复计分
--     §1.2 🟡 P1 semesters.is_current 标错（指向 2024-2025-1）+ 脏学期 2026-2029-2改
--     §1.5 🟡 P1 无对比学期时 change 落库为 0 → 接口返回 "+0"（应为 null）
--   本脚本负责存量数据修复；对应的代码改动（计分引擎新增 weighted_gpa 数据源、
--   无对比学期时 change 写 null、登录入口校验）见 `接口修改记录.md`。
--
-- 为什么不是 Flyway 迁移：
--   清单里的是**存量业务数据**修复，写成迁移会在所有环境（含生产）自动执行，
--   风险不可控；且 Flyway 已应用的迁移文件受校验和约束不得改写。沿用
--   `repair_political_status.sql` 的既有做法：独立、幂等、可重复执行的脚本。
--   新增 schema 变更仍应走新的 V{n} 迁移。
--
-- 幂等性：全部语句带"已修则不再匹配"的条件，可重复执行；
--   重复执行后 `SELECT` 校验段结果不变。
-- 事务：整体单事务，末尾 COMMIT；如需放弃，执行末尾的 ROLLBACK 段（注释掉的版本）。
-- ============================================================

START TRANSACTION;

-- ------------------------------------------------------------
-- 0. 执行前留档（只读，便于回滚核对；不需要可整段跳过）
-- ------------------------------------------------------------
SELECT '=== [备份] 受影响指标（修复前） ===' AS section;
SELECT `id`, `parent_id`, `indicator_code`, `indicator_name`, `weight`, `scoring_rule`
FROM `evaluation_indicators`
WHERE `id` IN (53, 60);

SELECT '=== [备份] 学期（修复前） ===' AS section;
SELECT `id`, `name`, `is_current`, `start_date`, `end_date`, `deleted_at`
FROM `semesters`
WHERE `deleted_at` IS NULL;

SELECT '=== [备份] 脏 change 计数（修复前） ===' AS section;
SELECT COUNT(*) AS `total`,
       SUM(`compared_semester_id` IS NULL) AS `no_compare`,
       SUM(`change` IS NOT NULL AND `compared_semester_id` IS NULL) AS `dirty`
FROM `portrait_evaluation_scores`;


-- ------------------------------------------------------------
-- 1. §1.1 重复指标：保留 id=60（竞赛实践→社会实践，语义正确），
--    把 id=53（学业成绩→绩点排名）改造为「绩点达标」
-- ------------------------------------------------------------
-- 决策依据（用户确认）：两个节点同名是**线上手工配置**造成的配置错误
--   （全仓库无任何 SQL 播种这两个节点，见清单 §1.1 取证）。
--   保留哪一处：id=60 位于「社会实践」二级节点下，与「志愿时长」语义天然吻合 → 保留。
--   改造 id=53：其父节点 A-2 的说明本就是「学分加权绩点与班级/专业排名」，
--   改为「绩点达标」后语义自洽，且**不动任何权重**：
--     一级 A(0.5) = A-1(0.3) + A-2(0.2)，A-2 子节点 {53: 0.2} 和 = 0.2   ✔
--     一级 B(0.3) = B-1(0.15) + B-2(0.15)，B-2 子节点 {60:0.1, 61:0.05} 和 = 0.15 ✔
--   因此各维度 targetScore 与 maxTotalScore(=100) 均不变，**前端零改动**
--   （这正是清单 §1.1「请后端②」推荐的那条路）。
--
-- 计分规则改用 source=weighted_gpa（学分加权绩点，Σ(gpa×credit)/Σ(credit)），
-- 该数据源由本轮 AdminScoreService.resolveSource 新增的 case 提供；
-- 阈值 3.0 取自本库 gpa_records 实际分布（2.60~4.50，均值 3.65）。
-- 注意：`indicator_code` 保持 A-2-1 不变 —— 它有唯一约束
--   uk_ei_code(school_id, indicator_code, is_deleted_null)，且历史快照按编码检索，
--   改编码属高风险的连带改动，与本次修复目标无关。
UPDATE `evaluation_indicators`
SET `indicator_name` = '绩点达标',
    `description`   = '学分加权绩点 Σ(gpa×credit)/Σ(credit) 达到 3.0 及以上得 100 分，未达标得 0 分',
    `scoring_rule`  = CAST('{"type":"THRESHOLD","source":"weighted_gpa","threshold":3.0,"score":100}' AS JSON),
    `updated_at`    = NOW()
WHERE `id` = 53
  AND `deleted_at` IS NULL
  AND `indicator_name` = '志愿时长达标';   -- 幂等：已改造过则不再匹配

-- 说明：`indicator_rule_versions.tree_snapshot` 是**发布时点冻结的历史快照**，
--   本脚本不修改它。要让本次改动生效到评分，必须由管理员**重新发布一个规则版本**
--   （见文末「后续步骤」），发布时系统会从当前草稿树 evaluation_indicators 重建快照。
--   为什么不直接改快照：① 历史版本是审计凭据，改写会让"当时发布的是什么"不可考；
--   ② 快照修补接口 (PATCH /admin/indicators/rule-versions/{id}/snapshot) 明确
--   禁止改 weight / scoringRule，改快照等于绕过发布校验（清单 §1.1 已指出权重链校验的意义）。


-- ------------------------------------------------------------
-- 2. §1.2 is_current 标到真正的当前学期，并清理脏学期 2026-2029-2改
-- ------------------------------------------------------------
-- 2.1 当前学期按**学期名 2025-2026-2**（id=4）认定 —— 与 seed_semesters.sql 的意图一致
--     （用户确认口径；该校当前只有一所学校 school_id=1，故不按校分别处理）。
UPDATE `semesters`
SET `is_current` = CASE WHEN `name` = '2025-2026-2' THEN 1 ELSE 0 END
WHERE `deleted_at` IS NULL
  AND COALESCE(`is_current`, -1) <> CASE WHEN `name` = '2025-2026-2' THEN 1 ELSE 0 END;  -- 幂等

-- 2.2 脏学期 2026-2029-2改（起止 2028-09-01 ~ 2029-01-21，明显异常）：
--     按名称定位（不写死 id），先记下它的 id 供后续连带清理使用。
SET @dirty_semester_id = (
    SELECT `id` FROM `semesters`
    WHERE `name` = '2026-2029-2改'
    ORDER BY `id` DESC LIMIT 1
);

-- 2.3 该学期在业务表里已有存量数据（一次 2026-09-04 的测试重算产出：
--     7 条 score_calculations、21 条 portrait_evaluation_scores、21 条 data_completeness、
--     1 条 score_recalculation_tasks）。若只软删学期行，这些记录会变成指向已删学期的孤儿，
--     故一并软删（**软删可回滚**，不是物理删除）。
UPDATE `score_calculations`
SET `deleted_at` = NOW()
WHERE `semester_id` = @dirty_semester_id AND `deleted_at` IS NULL;

UPDATE `portrait_evaluation_scores`
SET `deleted_at` = NOW()
WHERE `semester_id` = @dirty_semester_id AND `deleted_at` IS NULL;

UPDATE `data_completeness`
SET `deleted_at` = NOW()
WHERE `semester_id` = @dirty_semester_id AND `deleted_at` IS NULL;

UPDATE `score_recalculation_tasks`
SET `deleted_at` = NOW()
WHERE `semester_id` = @dirty_semester_id AND `deleted_at` IS NULL;

-- 2.4 最后软删学期行本身
UPDATE `semesters`
SET `is_current` = 0, `deleted_at` = NOW()
WHERE `id` = @dirty_semester_id AND `deleted_at` IS NULL;


-- ------------------------------------------------------------
-- 3. §1.5 存量清理：无对比学期时 change 应为 NULL（此前落库为 0 → 接口返回 "+0"）
-- ------------------------------------------------------------
-- 代码侧的根因已改为：AdminScoreService 不再以 ZERO 兜底，无上一学期时写 NULL；
-- 本段负责清理历史批次已经写脏的 0。
UPDATE `portrait_evaluation_scores`
SET `change` = NULL
WHERE `compared_semester_id` IS NULL
  AND `change` IS NOT NULL;   -- 幂等：已是 NULL 的行不再匹配


COMMIT;

-- 如需放弃本次修改：把上面的 COMMIT 换成 ROLLBACK 重跑本脚本（备份段为只读，不影响）；
-- 或在 COMMIT 之前执行 ROLLBACK;。软删的部分也可手工回滚：
--   UPDATE `score_calculations`          SET `deleted_at` = NULL WHERE `deleted_at` = <执行时刻>;
--   UPDATE `portrait_evaluation_scores`  SET `deleted_at` = NULL WHERE `deleted_at` = <执行时刻>;
--   UPDATE `data_completeness`           SET `deleted_at` = NULL WHERE `deleted_at` = <执行时刻>;
--   UPDATE `score_recalculation_tasks`   SET `deleted_at` = NULL WHERE `deleted_at` = <执行时刻>;
--   UPDATE `semesters`                   SET `deleted_at` = NULL WHERE `name` = '2026-2029-2改';
--   （§1.1 / §1.5 的字段回滚见文末「回滚」段。）


-- ============================================================
-- 4. 修复后校验（期望：indicators 名称不再重复；只有一个 is_current=1；dirty=0）
-- ============================================================
SELECT '=== [校验] 指标命名（期望 id=53 绩点达标 / id=60 志愿时长达标） ===' AS section;
SELECT `id`, `indicator_code`, `indicator_name`, `weight`, `scoring_rule`
FROM `evaluation_indicators`
WHERE `id` IN (53, 60);

SELECT '=== [校验] 同名跨维度残留（期望：空集） ===' AS section;
SELECT `indicator_name`, COUNT(DISTINCT `dimension_code`) AS `dim_cnt`, GROUP_CONCAT(`id`) AS `ids`
FROM `evaluation_indicators`
WHERE `deleted_at` IS NULL AND `status` = 1
GROUP BY `indicator_name`
HAVING `dim_cnt` > 1;

SELECT '=== [校验] 学期（期望：仅 2025-2026-2 的 is_current=1，无脏学期，无脏学期数据） ===' AS section;
SELECT `id`, `name`, `is_current`, `deleted_at` FROM `semesters` ORDER BY `id`;

SELECT '=== [校验] 脏学期残留数据（期望：全为 0） ===' AS section;
SELECT (SELECT COUNT(*) FROM `score_calculations`         WHERE `deleted_at` IS NULL AND `semester_id` = @dirty_semester_id) AS `calc_left`,
       (SELECT COUNT(*) FROM `portrait_evaluation_scores` WHERE `deleted_at` IS NULL AND `semester_id` = @dirty_semester_id) AS `portrait_left`,
       (SELECT COUNT(*) FROM `data_completeness`          WHERE `deleted_at` IS NULL AND `semester_id` = @dirty_semester_id) AS `completeness_left`,
       (SELECT COUNT(*) FROM `score_recalculation_tasks`  WHERE `deleted_at` IS NULL AND `semester_id` = @dirty_semester_id) AS `task_left`;

SELECT '=== [校验] 脏 change 残留（期望 dirty=0） ===' AS section;
SELECT COUNT(*) AS `total`,
       SUM(`change` IS NOT NULL AND `compared_semester_id` IS NULL) AS `dirty`
FROM `portrait_evaluation_scores`
WHERE `deleted_at` IS NULL;


-- ============================================================
-- 5. 后续步骤（脚本不会自动执行，需管理员操作）
-- ============================================================
-- 5.1 【必做】重新发布指标规则版本，让 §1.1 的改造进入评分口径。
--     当前学期 2025-2026-2 生效的是 indicator_rule_versions.id=3（version=2，
--     semester_id=4），其 tree_snapshot 里仍是**改造前**的重复节点 ——
--     评分引擎 AdminScoreService#resolveRuleForSemester 读的是快照而非活表，
--     所以"只改数据不重发"不会改变任何评分结果。
--
--     发布**必须不传 sourceVersionId**（走"基于当前草稿树发布"分支，从活表重建快照）：
--       POST /admin/indicators/publish
--       { "semesterId": 4, "versionName": "2026春-第2版" }
--     反例：若传 sourceVersionId=3，系统会深拷贝那条**旧快照**（仍含重复+旧规则），
--     改造不会生效。
--
-- 5.2 【必做】触发一次评分重算，使 §1.1 与 §1.5 反映到评分结果：
--       POST /admin/scores/recalculate   （或重跑受影响学生的重算任务）
--     重算后应观察到：indicatorId=53 为「绩点达标」（绩点≥3.0 得 100）、
--     id=60 仍为「志愿时长达标」，不再同源重复计分；
--     无对比学期的 change 为 null（接口不再返回 "+0"）。
--
-- 5.3 【建议】is_current 目前靠人工标记，容易再次标错。加唯一约束需 DDL，
--     属 schema 变更，应另行提交 V{n} 迁移（本脚本不做）；或由后端按日期推导当前学期。
--     清单 §1.2 的这条建议已记录在 `数据库修改记录.md`，留待排期。


-- ============================================================
-- 6. 回滚（字段级）
-- ============================================================
-- -- §1.1 回滚为改造前的「志愿时长达标」
-- UPDATE `evaluation_indicators`
-- SET `indicator_name` = '志愿时长达标',
--     `description`   = '志愿服务时长达到40小时，满分100分',
--     `scoring_rule`  = CAST('{"type":"THRESHOLD","source":"volunteer_hours","threshold":40,"score":100}' AS JSON)
-- WHERE `id` = 53;
--
-- -- §1.2 回滚学期标记（恢复为错误的旧状态，仅供紧急回退，回滚后请尽快重跑本脚本 2.1）
-- UPDATE `semesters` SET `is_current` = 1 WHERE `name` = '2024-2025-1';
-- UPDATE `semesters` SET `is_current` = 0 WHERE `name` = '2025-2026-2';
--
-- -- §1.5 无法逐行恢复（0 与原值不可区分），如需恢复请从执行前的备份表还原 change 列。
-- -- 建议：如需保留原值，执行本脚本前先
-- --   CREATE TABLE bak_pes_20261008 AS SELECT id, `change` FROM `portrait_evaluation_scores`
-- --   WHERE `compared_semester_id` IS NULL AND `change` IS NOT NULL;
