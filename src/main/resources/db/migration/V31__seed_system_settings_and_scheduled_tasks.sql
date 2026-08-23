-- ============================================================
-- V31: 系统配置与定时任务默认数据种子
-- 对齐《管理端接口文档》十二/十四，以及《学生档案系统表》scheduled_tasks 设计说明。
-- 使用 INSERT IGNORE 保证幂等（共享 dev 库已存在同键数据时跳过，不报错）。
-- ============================================================

-- 1. 系统配置（system_settings）
--    仅预置文档 12.1 示例中明确出现的配置项；其余配置键由运维按需补充。
INSERT IGNORE INTO `system_settings`
    (`setting_key`, `setting_name`, `setting_value`, `setting_group`, `value_type`, `description`, `is_editable`)
VALUES
    ('score.calculation.retry.max', '评分重算最大重试次数', '3', 'score', 'int', '评分重算任务失败后的最大重试次数', 1);

-- 2. 系统内置定时任务（scheduled_tasks，is_system=1）
--    对齐《学生档案系统表》scheduled_tasks 设计说明中的 4 个内置任务。
--    task_handler 暂以 task_code 作为处理器标识（当前版本尚未接入实际调度器，仅作配置占位）。
INSERT IGNORE INTO `scheduled_tasks`
    (`school_id`, `task_name`, `task_code`, `task_group`, `cron_expression`, `task_handler`, `description`, `is_system`, `run_type`, `max_retries`, `retry_delay_sec`, `timeout_sec`, `status`)
VALUES
    (1, '孤儿文件清理',     'orphan_file_cleanup',       'cleanup',      '0 3 * * *', 'orphan_file_cleanup',       '清理过期暂存文件与已软删文件', 1, 1, 0, 60, 0, 1),
    (1, '统计缓存刷新',     'statistics_refresh',        'data',         '0 6 * * *', 'statistics_refresh',        '刷新统计看板两级缓存',         1, 1, 0, 60, 0, 1),
    (1, '过期下载链接清理', 'expired_download_cleanup',  'cleanup',      '0 4 * * *', 'expired_download_cleanup',  '清理过期下载链接与文件',       1, 1, 0, 60, 0, 1),
    (1, '审批超时提醒',     'approval_timeout_remind',   'notification', '0 * * * *', 'approval_timeout_remind',   '每小时提醒审批超时待办',       1, 1, 0, 60, 0, 1);