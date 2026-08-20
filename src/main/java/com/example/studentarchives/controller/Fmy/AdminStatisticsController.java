package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.statistics.response.DashboardResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.HeatmapResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.OrgOverviewResponse;
import com.example.studentarchives.dto.Fmy.statistics.response.SnapshotRefreshResponse;
import com.example.studentarchives.service.Fmy.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端统计看板与可视化控制器
 * <p>
 * 对应《管理端接口文档》十六、统计看板与可视化模块（16.1 学校整体档案汇总 /
 * 16.2 组织下钻多维汇总 / 16.3 成果热力图数据），统一前缀 /admin/statistics，
 * 权限码 {@code statistics:view}。
 * <p>
 * 所有接口通过 {@link AdminStatisticsService} 校验 admin 角色或 statistics:view 权限码。
 * 响应头 {@code X-Cache-Hit} 标记数据来源（L2=命中统计缓存，MISS=读快照/实时聚合），
 * 与文档缓存说明保持一致；学校范围由当前登录用户推导。
 */
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    /** 缓存命中响应头 */
    private static final String CACHE_HIT_HEADER = "X-Cache-Hit";

    private final AdminStatisticsService adminStatisticsService;

    // ==================== 16.1 学校整体档案汇总（统计看板） ====================

    /**
     * 学校整体档案汇总（GET /admin/statistics/dashboard，文档 16.1）
     * <p>
     * 返回学校整体 KPI（学生/档案/获奖/绩点/审批状态/数据完整度）与多维概览
     * （维度得分/类型分布/热门兴趣）。数据优先命中 statistics_cache，其次读
     * org_archive_summaries 学校级快照。
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @return 看板数据，响应头携带 X-Cache-Hit
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<DashboardResponse>> dashboard(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        AdminStatisticsService.StatsResult<DashboardResponse> result =
                adminStatisticsService.dashboard(userId, semesterId);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }

    // ==================== 16.2 组织下钻多维汇总 ====================

    /**
     * 组织下钻多维汇总（GET /admin/statistics/overview，文档 16.2）
     * <p>
     * scopeType 为下钻维度（1=学校 2=学院 3=专业 4=班级 6=年级），rows 返回
     * 其下一级组织的多维汇总；不传默认按学院维度。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param scopeType  下钻维度（可选）
     * @param scopeId    当前组织 ID（可选，下钻其下一级）
     * @param grade      年级筛选（可选）
     * @return 组织多维汇总，响应头携带 X-Cache-Hit
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResult<OrgOverviewResponse>> overview(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "grade", required = false) String grade) {
        AdminStatisticsService.StatsResult<OrgOverviewResponse> result =
                adminStatisticsService.overview(userId, semesterId, scopeType, scopeId, grade);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }

    // ==================== 16.3 成果热力图数据 ====================

    /**
     * 成果热力图数据（GET /admin/statistics/heatmap，文档 16.3）
     * <p>
     * 以组织单位为行、指标/学期为列返回热力图矩阵，数值按全校该指标最大值
     * 归一化到 0-100。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传按全校启用学期展开列）
     * @param orgType    行维度：2=学院 3=专业 4=班级（必填）
     * @param orgId      上级组织 ID（可选，返回其下各组织行）
     * @param metric     指标：gpa/award/practice/interest/archive（必填）
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
                adminStatisticsService.heatmap(userId, semesterId, orgType, orgId, metric, grade);
        return ResponseEntity.ok()
                .header(CACHE_HIT_HEADER, result.cacheHit())
                .body(ApiResult.success(result.data()));
    }

    // ==================== 16.4 手动刷新统计快照 ====================

    /**
     * 手动刷新学校级档案汇总快照（POST /admin/statistics/refresh，文档 16.4）
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<SnapshotRefreshResponse>> refreshSnapshot(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        SnapshotRefreshResponse response = adminStatisticsService.refreshSnapshot(userId, semesterId);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
