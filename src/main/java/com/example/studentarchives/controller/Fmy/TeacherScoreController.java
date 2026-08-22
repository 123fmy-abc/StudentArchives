package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.score.request.ScoreRecalculateRequest;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculateResponse;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculationTaskResponse;
import com.example.studentarchives.service.Fmy.TeacherScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端评分重算控制器
 * <p>
 * 提供教师端评分重算模块接口（《教师端接口文档》十六、统计看板模块 - 评分重算），
 * 统一前缀 /teacher/scores：
 * <ul>
 *   <li>11.4 POST /teacher/scores/recalculate：复用管理端 2.1 异步引擎
 *       （{@code AdminScoreService.triggerRecalculateByTeacher}），targetType 仅 1/2/3，
 *       按 role_scopes 校验范围；</li>
 *   <li>11.5 GET /teacher/scores/recalculation-tasks/{taskId}：复用管理端 2.2 查询逻辑，
 *       仅本人触发的任务可查。</li>
 * </ul>
 * 权限口径：管理员放行或持有 {@code score:recalculate} 权限码，越权返回 20005。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/scores")
@RequiredArgsConstructor
public class TeacherScoreController {

    private final TeacherScoreService teacherScoreService;

    /**
     * 触发评分重算（POST /teacher/scores/recalculate，教师端文档 11.4）
     *
     * @param userId  当前登录用户 ID（由 JWT 过滤器注入）
     * @param request 触发请求：targetType（1=指定学生 2=指定班级 3=指定学期）、
     *                targetId（targetType=1/2 时必填）、semesterId
     * @return 任务 ID 与初始状态
     */
    @AuditLog(module = "score", action = "recalculate",
            description = "教师端触发评分重算: targetType=#request.targetType, targetId=#request.targetId", logResult = true)
    @PostMapping("/recalculate")
    public ApiResult<ScoreRecalculateResponse> triggerRecalculate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScoreRecalculateRequest request) {
        ScoreRecalculateResponse response = teacherScoreService.triggerRecalculate(userId, request);
        return ApiResult.success("评分重算任务已创建", response);
    }

    /**
     * 查询评分重算任务进度（GET /teacher/scores/recalculation-tasks/{taskId}，教师端文档 11.5）
     *
     * @param userId 当前登录用户 ID
     * @param taskId 评分重算任务 ID
     * @return 任务状态、进度与结果摘要
     */
    @GetMapping("/recalculation-tasks/{taskId:[0-9]+}")
    public ApiResult<ScoreRecalculationTaskResponse> getRecalculationTask(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId) {
        return ApiResult.success(teacherScoreService.getRecalculationTask(userId, taskId));
    }
}
