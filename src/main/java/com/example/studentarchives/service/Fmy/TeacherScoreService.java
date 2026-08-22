package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.score.request.ScoreRecalculateRequest;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculateResponse;
import com.example.studentarchives.dto.Fmy.score.response.ScoreRecalculationTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 教师端评分重算服务（《教师端接口文档》十六、统计看板模块 - 评分重算）
 * <p>
 * 两个接口均复用管理端 {@link AdminScoreService} 的引擎（11.4/11.5）：
 * <ul>
 *   <li>触发评分重算：{@link AdminScoreService#triggerRecalculateByTeacher}——复用 2.1 异步引擎，
 *       教师侧限定 targetType 仅 1/2/3 并按 role_scopes 校验范围；</li>
 *   <li>查询任务进度：{@link AdminScoreService#getRecalculationTaskByTeacher}——复用 2.2 查询逻辑，
 *       教师侧补充任务归属校验（仅本人触发）。</li>
 * </ul>
 * 越权返回 20005 无访问权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherScoreService {

    private final AdminScoreService adminScoreService;

    /**
     * 触发评分重算（POST /teacher/scores/recalculate，教师端文档 11.4）
     *
     * @param userId  当前教师用户 ID
     * @param request 触发请求（targetType 仅 1=指定学生 2=指定班级 3=指定学期）
     * @return 任务 ID 与初始状态
     */
    public ScoreRecalculateResponse triggerRecalculate(Long userId, ScoreRecalculateRequest request) {
        return adminScoreService.triggerRecalculateByTeacher(userId, request);
    }

    /**
     * 查询评分重算任务进度（GET /teacher/scores/recalculation-tasks/{taskId}，教师端文档 11.5）
     *
     * @param userId 当前教师用户 ID
     * @param taskId 评分重算任务 ID
     * @return 任务状态、进度与结果摘要
     */
    public ScoreRecalculationTaskResponse getRecalculationTask(Long userId, Long taskId) {
        return adminScoreService.getRecalculationTaskByTeacher(userId, taskId);
    }
}
