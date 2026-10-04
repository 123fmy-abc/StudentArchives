package com.example.studentarchives.service.schedule;

import com.example.studentarchives.service.Fmy.StatisticsSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 统计快照刷新处理器（task_code = statistics_refresh）。
 * <p>
 * 由动态调度器按 {@code scheduled_tasks} 中该任务的 cron（seed: 每日 06:00）触发，
 * 每个学校一条任务记录，处理器仅刷新 {@link TaskExecutionContext#getSchoolId()} 对应学校的统计快照。
 */
@Component
@RequiredArgsConstructor
public class StatisticsRefreshHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "statistics_refresh";

    private final StatisticsSnapshotService statisticsSnapshotService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "定时刷新学校统计数据快照（系统内置）";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        statisticsSnapshotService.refresh(context.getSchoolId(), null);
    }
}
