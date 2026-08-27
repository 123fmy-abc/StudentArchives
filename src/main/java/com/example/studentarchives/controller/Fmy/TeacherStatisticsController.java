package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.SnapshotRefreshResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.TeacherDashboardResponse;
import com.example.studentarchives.service.Fmy.AdminStatisticsService;
import com.example.studentarchives.service.Fmy.TeacherStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
 * 教师登录即可访问，数据范围按 {@code role_scopes} 授权校验（越权返回 20005）。
 * 响应头 X-Cache-Hit 标记数据来源（与管理端统计口径一致：MISS=读快照/实时聚合）。
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
     * @param grade      年级（scopeType=6 时必填，如 {@code 2024级}；与 7.1/12.4.2 的 grade 口径一致）
     * @return 看板数据，响应头携带 X-Cache-Hit
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<TeacherDashboardResponse>> dashboard(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "grade", required = false) String grade) {
        AdminStatisticsService.StatsResult<TeacherDashboardResponse> result =
                teacherStatisticsService.getDashboard(userId, scopeType, scopeId, semesterId, grade);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }

    /**
     * 班级/专业级成果热力图（GET /teacher/statistics/heatmap，教师端文档 11.3）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（传则仅返回单学期列）
     * @param orgType    行维度：2=学院 3=专业 4=班级（不传默认班级维度）
     * @param orgId      上级组织 ID（可选，下钻；不传返回该维度授权范围内全部组织）
     * @param metric     指标：gpa/award/practice/archive（不传默认 award）
     * @param grade      年级筛选（可选，不传取教师主职授权范围内主要年级）
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

    /**
     * 手动刷新学校级统计快照（POST /teacher/statistics/refresh，教师端文档 12.5.5）
     * <p>
     * 复用管理端 16.4 刷新引擎，重新聚合学校级与学院/专业/班级行级快照；教师登录即可，
     * 需存在生效授权范围（role_scopes），越权返回 20005。刷新后 12.5.1 看板的
     * averageGpa / dimensionAvgScores / archiveTypeDistribution 等快照字段即有数据。
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param semesterId 学期 ID（不传取当前学期）
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<SnapshotRefreshResponse>> refreshSnapshot(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        return ResponseEntity.ok(ApiResult.success(teacherStatisticsService.refreshSnapshot(userId, semesterId)));
    }
}
