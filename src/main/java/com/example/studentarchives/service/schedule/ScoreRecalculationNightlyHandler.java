package com.example.studentarchives.service.schedule;

import com.example.studentarchives.service.Fmy.AdminScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 每日画像评分兜底重算处理器（task_code = score_recalculation_nightly）。
 * <p>
 * 由动态调度器按 {@code scheduled_tasks} 中该任务的 cron（seed: 每日 02:30）触发，
 * 每个学校一条任务记录，处理器仅处理 {@link TaskExecutionContext#getSchoolId()} 对应学校。
 */
@Component
@RequiredArgsConstructor
public class ScoreRecalculationNightlyHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "score_recalculation_nightly";

    private final AdminScoreService adminScoreService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "每日兜底重算当前学期画像评分（系统内置）";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        adminScoreService.recalculateStaleStudentsForCurrentSemester(context.getSchoolId());
    }
}
