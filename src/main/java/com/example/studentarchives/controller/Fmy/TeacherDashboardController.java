package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.home.response.TeacherDashboardOverviewResponse;
import com.example.studentarchives.service.Fmy.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师首页控制器（《教师端接口文档》三、教师首页数据概览）
 * <p>
 * 提供教师端首页数据概览接口，统一前缀 /teacher。所有接口需携带 Bearer Token 认证。
 */
@Slf4j
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    /**
     * 教师首页数据概览（GET /teacher/dashboard，教师端文档 3.1）
     * <p>
     * 聚合当前教师的真实数据：教师信息、当前学期、授权范围、待办统计、
     * 今日审核数、最近审核动态、未读消息数。
     *
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 教师首页概览
     */
    @GetMapping("/dashboard")
    public ApiResult<TeacherDashboardOverviewResponse> getDashboard(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(teacherDashboardService.getDashboard(userId));
    }
}
