package com.example.studentarchives.service.schedule;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.service.Fmy.AdminScoreService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 指标发布后全量重算处理器（task_code = score_recalculation_on_publish）。
 * <p>
 * run_type=2（手动触发型），由 14.7 手动触发或指标发布事件驱动，不参与动态 cron 调度。
 * 学期解析优先级：runParams.semesterId &gt; taskParams.semesterId &gt; 学校当前学期。
 */
@Component
@RequiredArgsConstructor
public class ScoreRecalculationOnPublishHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "score_recalculation_on_publish";

    private final AdminScoreService adminScoreService;
    private final SemesterRepository semesterRepository;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "指标发布后触发全量画像评分重算（系统内置，通常由事件触发）";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        adminScoreService.triggerSemesterRecalculation(context.getSchoolId(), resolveSemesterId(context), null);
    }

    private Long resolveSemesterId(TaskExecutionContext context) {
        Map<String, Object> runParams = context.getRunParams();
        if (runParams != null && runParams.get("semesterId") instanceof Number n) {
            return n.longValue();
        }
        JsonNode taskParams = context.getTaskParams();
        if (taskParams != null && taskParams.hasNonNull("semesterId")) {
            return taskParams.get("semesterId").asLong();
        }
        Semester semester = semesterRepository.findCurrentBySchoolId(context.getSchoolId()).orElse(null);
        if (semester == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "学校无当前学期，请通过 runParams.semesterId 指定学期");
        }
        return semester.getId();
    }
}
