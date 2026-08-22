package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.TeacherDashboardResponse;
import com.example.studentarchives.service.Fmy.AdminStatisticsService;
import com.example.studentarchives.service.Fmy.TeacherStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端统计看板控制器
 * <p>
 * 提供教师端统计看板模块接口（《教师端接口文档》十六、统计看板模块），统一前缀
 * /teacher/statistics：
 * <ul>
 *   <li>11.1 GET /teacher/statistics/dashboard：复用 org_archive_summaries 快照 +
 *       statistics_cache 聚合基础，教师侧按 role_scopes 限定范围；</li>
 *   <li>11.3 GET /teacher/statistics/heatmap：复用管理端 16.3 热力图引擎，教师侧限定组织行。</li>
 * </ul>
 * 权限码 {@code statistics:view}，越权返回 20005。响应头 X-Cache-Hit 标记数据来源
 * （与管理端统计口径一致：MISS=读快照/实时聚合）。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/statistics")
@RequiredArgsConstructor
public class TeacherStatisticsController {

    /** 缓存命中响应头 */
    private static final String CACHE_HIT_HEADER = "X-Cache-Hit";

    private final TeacherStatisticsService teacherStatisticsService;

    /**
     * 教师范围统计看板（GET /teacher/statistics/dashboard，教师端文档 11.1）
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param scopeType  范围类型：1学校 2学院 3专业 4班级 6年级（不传取教师首个生效授权范围）
     * @param scopeId    范围 ID
     * @param semesterId 学期 ID（不传取当前学期）
     * @return 看板数据，响应头携带 X-Cache-Hit
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<TeacherDashboardResponse>> dashboard(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        AdminStatisticsService.StatsResult<TeacherDashboardResponse> result =
                teacherStatisticsService.getDashboard(userId, scopeType, scopeId, semesterId);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }

    /**
     * 班级/专业级成果热力图（GET /teacher/statistics/heatmap，教师端文档 11.3）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（传则仅返回单学期列）
     * @param orgType    行维度：2=学院 3=专业 4=班级
     * @param orgId      上级组织 ID（可选，下钻）
     * @param metric     指标：gpa/award/practice/archive
     * @param grade      年级筛选（可选）
     * @return 热力图矩阵，响应头携带 X-Cache-Hit
     */
    @GetMapping("/heatmap")
    public ResponseEntity<ApiResult<HeatmapResponse>> heatmap(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "orgType", required = false) Integer orgType,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "metric", required = false) String metric,
            @RequestParam(value = "grade", required = false) String grade) {
        AdminStatisticsService.StatsResult<HeatmapResponse> result =
                teacherStatisticsService.getHeatmap(userId, semesterId, orgType, orgId, metric, grade);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }
}
