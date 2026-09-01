package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.publicstats.response.PublicStatisticsResponse;
import com.example.studentarchives.service.Fmy.PublicStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录页公开统计控制器
 * <p>
 * 提供登录页品牌区底部统计数字接口，免鉴权（未登录可访问），
 * 路径 /public/** 已加入 SecurityConstants.PUBLIC_AUTH_PATHS。
 */
@Slf4j
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicStatisticsController {

    private final PublicStatisticsService publicStatisticsService;

    /**
     * 登录页公开统计概览
     * <p>
     * 免鉴权接口，返回在校学生数、档案条目数、待审申请数及可用率/稳定率，
     * 供学生端与管理端登录页展示真实数据概览。
     *
     * @return 登录页统计概览
     */
    @GetMapping("/statistics")
    public ApiResult<PublicStatisticsResponse> getStatistics() {
        PublicStatisticsResponse response = publicStatisticsService.getStatistics();
        return ApiResult.success(response);
    }
}
