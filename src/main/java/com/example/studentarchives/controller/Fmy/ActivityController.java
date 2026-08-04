package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.activity.request.ActivityListRequest;
import com.example.studentarchives.dto.Fmy.activity.response.ActivityDetailResponse;
import com.example.studentarchives.dto.Fmy.activity.response.ActivityListItemResponse;
import com.example.studentarchives.dto.Fmy.activity.response.ActivityStatusResponse;
import com.example.studentarchives.enums.ActivityTypeEnum;
import com.example.studentarchives.service.Fmy.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态记录控制器
 * <p>
 * 统一管理学生档案、奖项报名、职业规划的查询、编辑、删除与撤回操作。
 * 所有接口需携带 Bearer Token 认证。
 * <p>
 * 采用路径变量法区分来源类型：/{type}/{activityId}，type 取值：archive / award / career_plan。
 */
@Slf4j
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 获取动态记录列表（可跨 3 表筛选）
     */
    @GetMapping
    public ApiResult<PageResult<ActivityListItemResponse>> list(
            ActivityListRequest request,
            @AuthenticationPrincipal Long userId) {
        return activityService.list(request, userId).toApiResult();
    }

    /**
     * 获取动态记录详情
     */
    @GetMapping("/{type}/{activityId}")
    public ApiResult<ActivityDetailResponse> getDetail(
            @PathVariable String type,
            @PathVariable Long activityId,
            @AuthenticationPrincipal Long userId) {
        ActivityTypeEnum activityType = ActivityTypeEnum.of(type);
        return ApiResult.success(activityService.getDetail(activityType, activityId, userId));
    }

    /**
     * 编辑动态记录（仅限草稿/已退回状态，编辑后自动提交为待审批）
     */
    @PutMapping("/{type}/{activityId}")
    public ApiResult<ActivityStatusResponse> edit(
            @PathVariable String type,
            @PathVariable Long activityId,
            @RequestBody ActivityService.ActivityEditBody body,
            @AuthenticationPrincipal Long userId) {
        ActivityTypeEnum activityType = ActivityTypeEnum.of(type);
        return ApiResult.success("更新成功", activityService.edit(activityType, activityId, body, userId));
    }

    /**
     * 删除动态记录（软删除，仅限草稿/已退回状态）
     */
    @DeleteMapping("/{type}/{activityId}")
    public ApiResult<Void> delete(
            @PathVariable String type,
            @PathVariable Long activityId,
            @AuthenticationPrincipal Long userId) {
        ActivityTypeEnum activityType = ActivityTypeEnum.of(type);
        activityService.delete(activityType, activityId, userId);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 撤回申报（待审批 → 草稿）
     */
    @PutMapping("/{type}/{activityId}/withdraw")
    public ApiResult<ActivityStatusResponse> withdraw(
            @PathVariable String type,
            @PathVariable Long activityId,
            @AuthenticationPrincipal Long userId) {
        ActivityTypeEnum activityType = ActivityTypeEnum.of(type);
        return ApiResult.success("撤回成功", activityService.withdraw(activityType, activityId, userId));
    }
}
