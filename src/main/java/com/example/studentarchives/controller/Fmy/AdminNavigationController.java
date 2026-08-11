package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.navigation.response.NavigationItemResponse;
import com.example.studentarchives.service.Fmy.AdminNavigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端导航菜单控制器
 * <p>
 * 修复“菜单层级与接口模块对应关系”风险：后端按当前用户角色返回菜单树，
 * 将图片中管理员/审核员/任课教师的菜单层级映射到具体接口前缀，前端不再硬编码菜单。
 */
@Slf4j
@RestController
@RequestMapping("/admin/navigation")
@RequiredArgsConstructor
public class AdminNavigationController {

    private final AdminNavigationService adminNavigationService;

    /**
     * 获取当前用户菜单
     * <p>
     * 根据登录用户角色返回可见菜单树，包含档案查看、档案导出、角色选择、
     * 表单自定义、日志查看、成果热力图、账号管理等菜单及其对应后端路径。
     */
    @GetMapping
    public ApiResult<List<NavigationItemResponse>> getNavigation(
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(adminNavigationService.buildNavigation(userId));
    }
}
