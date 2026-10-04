package com.example.studentarchives.service.schedule;

/**
 * 定时任务处理器统一接口。
 * <p>
 * 每个定时任务只需实现本接口并注册为 Spring Bean（{@link #getTaskCode()} 与
 * {@code scheduled_tasks.task_code} 对应），即完成「业务执行逻辑」接入；调度、重试、超时、
 * 执行状态回写等横切能力由框架（{@link TaskExecutionDispatcher} / {@link DynamicTaskScheduler}）统一处理，
 * 无需在处理器内部重复实现。
 */
public interface ScheduledTaskHandler {

    /**
     * 处理器对应的任务编码，与 {@code scheduled_tasks.task_code} 一致。
     */
    String getTaskCode();

    /**
     * 执行任务业务逻辑。抛出异常视为本次执行失败，由框架按 max_retries / retry_delay_sec 重试。
     */
    void execute(TaskExecutionContext context) throws Exception;

    /**
     * 处理器展示名称（供前端下拉列表显示）。默认使用 Spring Bean 名（类名首字母小写）。
     */
    default String getHandlerName() {
        String simple = getClass().getSimpleName();
        if (simple.length() >= 2 && Character.isUpperCase(simple.charAt(0))
                && Character.isUpperCase(simple.charAt(1))) {
            return simple;
        }
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    /**
     * 处理器功能说明（供前端下拉列表显示）。默认返回空串，实现类可覆盖。
     */
    default String getDescription() {
        return "";
    }
}
