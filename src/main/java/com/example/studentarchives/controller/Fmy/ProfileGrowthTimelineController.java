package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.profile.request.GrowthTimelineCreateRequest;
import com.example.studentarchives.dto.Fmy.profile.request.GrowthTimelineUpdateRequest;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineDetailResponse;
import com.example.studentarchives.service.Fmy.ProfileGrowthTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心成长时间轴控制器（学生端 CRUD）
 * <p>
 * 在既有只读接口 {@code GET /profile/growth-timeline}（ProfileController）基础上，
 * 新增成长时间轴事件的新增 / 详情 / 修改 / 删除接口，统一前缀 {@code /profile/growth-timeline}。
 * 所有接口需携带 Bearer Token，且学生仅可操作本人数据（越权返回 20005）。
 */
@Slf4j
@RestController
@RequestMapping("/profile/growth-timeline")
@RequiredArgsConstructor
public class ProfileGrowthTimelineController {

    private final ProfileGrowthTimelineService profileGrowthTimelineService;

    /**
     * 新增成长时间轴事件（4.2.1）
     *
     * @param userId  当前登录用户 ID（由 JWT 过滤器注入）
     * @param request 新增请求
     * @return 新建后的时间轴事件详情
     */
    @PostMapping
    public ApiResult<GrowthTimelineDetailResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GrowthTimelineCreateRequest request) {
        return ApiResult.success("新增成功", profileGrowthTimelineService.create(userId, request));
    }

    /**
     * 获取成长时间轴事件详情（4.2.2）
     *
     * @param userId 当前登录用户 ID
     * @param id     时间轴节点 ID
     * @return 时间轴事件详情
     */
    @GetMapping("/{id:[0-9]+}")
    public ApiResult<GrowthTimelineDetailResponse> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ApiResult.success(profileGrowthTimelineService.getDetail(userId, id));
    }

    /**
     * 修改成长时间轴事件（4.2.3）
     *
     * @param userId  当前登录用户 ID
     * @param id      时间轴节点 ID
     * @param request 修改请求
     * @return 更新后的时间轴事件详情
     */
    @PutMapping("/{id:[0-9]+}")
    public ApiResult<GrowthTimelineDetailResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody GrowthTimelineUpdateRequest request) {
        return ApiResult.success("更新成功", profileGrowthTimelineService.update(userId, id, request));
    }

    /**
     * 删除成长时间轴事件（4.2.4）
     *
     * @param userId 当前登录用户 ID
     * @param id     时间轴节点 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id:[0-9]+}")
    public ApiResult<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        profileGrowthTimelineService.delete(userId, id);
        return ApiResult.success("删除成功", null);
    }
}
