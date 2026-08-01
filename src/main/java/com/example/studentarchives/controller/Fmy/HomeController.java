package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.home.response.DashboardResponse;
import com.example.studentarchives.service.Fmy.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 * <p>
 * 提供学生端首页数据概览接口，所有接口需携带 Bearer Token 认证。
 */
@Slf4j
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 首页数据概览
     * <p>
     * 聚合当前学生真实数据返回：学生信息、申报统计、学期成绩、
     * 画像指标、雷达图、数据完整度、快捷入口、最近动态、未读消息数。
     *
     * @param userId 当前登录用户 ID（由 JWT 过滤器注入）
     * @return 首页概览
     */
    @GetMapping("/dashboard")//SpringMVC 路由注解
    //方法名 getDashboard
    //入参 @AuthenticationPrincipal Long userId
    //@AuthenticationPrincipal 自动从当前登录认证上下文取出登录用户信息
    //Long userId 注解会把当前登录人的用户 ID 注入该参数
    public ApiResult<DashboardResponse> getDashboard(@AuthenticationPrincipal Long userId) {
        //homeService.getDashboard(userId):传入当前登录用户 ID，执行业务逻辑
        DashboardResponse response = homeService.getDashboard(userId);

        //ApiResult 是项目全局统一响应封装类
        //调用统一返回工具类的静态方法,ApiResult.success(数据) 内部自动组装完整返回结构
        //data = 传入的DashboardResponse对象
        return ApiResult.success(response);
    }
}
