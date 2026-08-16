package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.AdminLogService;
import com.example.studentarchives.service.Lzw.AdminLogService.ExportLogItem;
import com.example.studentarchives.service.Lzw.AdminLogService.ExportLogQuery;
import com.example.studentarchives.service.Lzw.AdminLogService.LoginLogItem;
import com.example.studentarchives.service.Lzw.AdminLogService.LoginLogQuery;
import com.example.studentarchives.service.Lzw.AdminLogService.SystemLogItem;
import com.example.studentarchives.service.Lzw.AdminLogService.SystemLogQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端操作日志模块（Lzw）
 * <p>
 * 对应《管理端接口文档》四、操作日志模块（4.1 ~ 4.3）。
 * 权限：读接口需 admin 角色或 log:view / log:manage。
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    // ==================== 4.1 查询系统操作日志 ====================

    @GetMapping("/system")
    public ApiResult<PageResult<SystemLogItem>> listSystemLogs(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "operatorId", required = false) Long qOperatorId,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "logLevel", required = false) Integer logLevel,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "relatedType", required = false) String relatedType,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "collegeId", required = false) Long collegeId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        SystemLogQuery query = SystemLogQuery.builder()
                .operatorId(qOperatorId)
                .roleId(roleId)
                .action(action)
                .module(module)
                .logLevel(logLevel)
                .startTime(startTime)
                .endTime(endTime)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .grade(grade)
                .collegeId(collegeId)
                .majorId(majorId)
                .classId(classId)
                .build();
        return ApiResult.success(adminLogService.listSystemLogs(operatorId, query, buildPageParam(page, perPage)));
    }

    // ==================== 4.2 查询登录日志 ====================

    @GetMapping("/login")
    public ApiResult<PageResult<LoginLogItem>> listLoginLogs(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "loginStatus", required = false) Integer loginStatus,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        LoginLogQuery query = LoginLogQuery.builder()
                .userId(userId)
                .loginStatus(loginStatus)
                .startTime(startTime)
                .endTime(endTime)
                .ipAddress(ipAddress)
                .build();
        return ApiResult.success(adminLogService.listLoginLogs(operatorId, query, buildPageParam(page, perPage)));
    }

    // ==================== 4.3 查询导出操作日志 ====================

    @GetMapping("/exports")
    public ApiResult<PageResult<ExportLogItem>> listExportLogs(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "operatorId", required = false) Long qOperatorId,
            @RequestParam(value = "exportType", required = false) String exportType,
            @RequestParam(value = "isAnonymized", required = false) Integer isAnonymized,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        ExportLogQuery query = ExportLogQuery.builder()
                .operatorId(qOperatorId)
                .exportType(exportType)
                .isAnonymized(isAnonymized)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return ApiResult.success(adminLogService.listExportLogs(operatorId, query, buildPageParam(page, perPage)));
    }

    private PageParam buildPageParam(int page, int perPage) {
        return PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
    }
}