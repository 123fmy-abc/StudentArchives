package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.log.request.SystemLogQueryRequest;
import com.example.studentarchives.dto.Fmy.log.response.SystemLogItemResponse;
import com.example.studentarchives.service.Fmy.AdminAuthService;
import com.example.studentarchives.service.Fmy.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端日志模块
 * <p>
 * 对应《管理端接口文档》三、日志模块。当前仅实现 GET /admin/logs/system（3.1），
 * 其余日志查询接口（登录日志/导出日志）为规划中。
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    /** 查看操作日志权限码（《管理端接口文档》关键权限码） */
    private static final String LOG_VIEW_PERMISSION = "log:view";

    private final AdminAuthService adminAuthService;
    private final SystemLogService systemLogService;

    /**
     * 查询系统操作日志（GET /admin/logs/system，管理端文档 3.1）
     * <p>
     * 按操作人、角色、时间、操作类型、模块分页筛选，数据来自 system_logs 表。
     * 需要 admin 角色或 log:view 权限码，越权返回 20005。
     */
    @GetMapping("/system")
    public ApiResult<PageResult<SystemLogItemResponse>> listSystemLogs(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "logLevel", required = false) Integer logLevel,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "relatedType", required = false) String relatedType,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        adminAuthService.requireAdminOrPermission(userId, LOG_VIEW_PERMISSION);

        SystemLogQueryRequest query = SystemLogQueryRequest.builder()
                .operatorId(operatorId)
                .roleId(roleId)
                .action(action)
                .module(module)
                .logLevel(logLevel)
                .startTime(startTime)
                .endTime(endTime)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .build();
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(systemLogService.listSystemLogs(query, pageParam));
    }
}
