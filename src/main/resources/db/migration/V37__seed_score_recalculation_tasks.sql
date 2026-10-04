-- ============================================================
-- V37: 画像评分自动化系统任务种子
-- 对齐《管理端接口文档》十四 14.0 系统内置任务概览。
-- 使用 INSERT IGNORE 保证幂等（共享 dev 库已存在同键数据时跳过，不报错）。
-- ============================================================

-- 评分自动化系统内置任务（scheduled_tasks，is_system=1，task_group=evaluation）
--   score_recalculation_nightly   : 每日画像评分兜底重算（run_type=1，由 @Scheduled 硬编码调度）
--   score_recalculation_on_publish: 指标发布后全量重算（run_type=2，由发布事件内部触发，
--                                   cron_expression 仅为满足 NOT NULL 约束的占位值，不实际调度）
INSERT IGNORE INTO `scheduled_tasks`
    (`school_id`, `task_name`, `task_code`, `task_group`, `cron_expression`, `task_handler`,
     `description`, `is_system`, `run_type`, `max_retries`, `retry_delay_sec`, `timeout_sec`, `status`)
VALUES
    (1, '每日画像评分兜底重算', 'score_recalculation_nightly',    'evaluation',
     '0 30 2 * * ?', 'score_recalculation_nightly',
     '每日兜底补算画像评分缺失或规则版本落后的学生', 1, 1, 2, 60, 1800, 1),
    (1, '指标发布后全量重算',   'score_recalculation_on_publish', 'evaluation',
     '0 0 3 1 1 ?', 'score_recalculation_on_publish',
     '指标发布时事件触发，自动重算该学期所有学生画像评分', 1, 2, 2, 60, 1800, 1);
