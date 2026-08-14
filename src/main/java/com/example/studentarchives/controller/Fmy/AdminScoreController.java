package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.score.request.ScoreRecalculateRequest;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculateResponse;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculationTaskResponse;
import com.example.studentarchives.service.Fmy.AdminScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端评分重算控制器
 * <p>
 * 对应《管理端接口文档》二、评分重算模块（2.1 触发评分重算 / 2.2 查询任务进度），
 * 统一前缀 /admin/scores。
 * <p>
 * 所有接口需通过 {@link AdminScoreService} 校验 admin 角色或 score:recalculate 权限码，
 * 越权返回 20005 无访问权限。触发重算写入 audit_log 审计日志（module=score, action=recalculate）。
 */
@RestController
@RequestMapping("/admin/scores")
@RequiredArgsConstructor
public class AdminScoreController {

    private final AdminScoreService adminScoreService;

    // ==================== 2.1 触发评分重算 ====================

    /**
     * 触发评分重算（POST /admin/scores/recalculate，管理端文档 2.1）
     * <p>
     * 管理员触发指定学生/班级/学期/专业/全量的评分重算。任务进入 score_recalculation_tasks
     * 异步执行，接口立即返回任务 ID。同一范围已有生效中任务时返回 41005。
     *
     * @param userId  当前登录用户 ID（由 JWT 过滤器注入）
     * @param request 触发请求：targetType（1=指定学生 2=指定班级 3=指定学期 4=全量重算 5=指定专业）、
     *                targetId（targetType=1/2/5 时必填）、semesterId
     * @return 任务 ID 与初始状态
     */
    @AuditLog(module = "score", action = "recalculate",
            description = "触发评分重算: targetType=#request.targetType, targetId=#request.targetId", logResult = true)
    @PostMapping("/recalculate")
    public ApiResult<ScoreRecalculateResponse> triggerRecalculate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScoreRecalculateRequest request) {
        ScoreRecalculateResponse response = adminScoreService.triggerRecalculate(userId, request);
        return ApiResult.success("评分重算任务已创建", response);
    }

    // ==================== 2.2 查询评分重算任务进度 ====================

    /**
     * 查询评分重算任务进度（GET /admin/scores/recalculation-tasks/{taskId}，管理端文档 2.2）
     * <p>
     * 返回任务状态（0=排队中 1=执行中 2=完成 3=失败）、进度百分比与结果摘要
     * （总数/成功数/失败数/起止时间/失败原因）。
     *
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @param taskId 评分重算任务 ID
     * @return 任务状态、进度与结果摘要
     */
    @GetMapping("/recalculation-tasks/{taskId}")
    public ApiResult<ScoreRecalculationTaskResponse> getRecalculationTask(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId) {
        ScoreRecalculationTaskResponse response = adminScoreService.getRecalculationTask(userId, taskId);
        return ApiResult.success(response);
    }
}
