package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskItem;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskStatusResponse;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端定时任务管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》十四、定时任务管理模块（14.1 ~ 14.2）。
 * 权限：要求 admin 角色（越权返回 20005），由 Service 层校验。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminScheduledTaskController {

    private final ScheduledTaskManageService scheduledTaskManageService;

    // ==================== 14.1 获取定时任务列表 ====================

    @GetMapping("/scheduled-tasks")
    public ApiResult<PageResult<ScheduledTaskItem>> listTasks(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "taskGroup", required = false) String taskGroup,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(scheduledTaskManageService.listTasks(operatorId, taskGroup, status, pageParam));
    }

    // ==================== 14.2 启停定时任务 ====================

    @AuditLog(module = "scheduled-task", action = "update-status", description = "启停定时任务: #taskId", relatedType = "scheduled-task", relatedId = "#taskId")
    @PutMapping("/scheduled-tasks/{taskId}/status")
    public ApiResult<ScheduledTaskStatusResponse> updateStatus(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long taskId,
            @RequestBody ScheduledTaskStatusUpdateRequest body) {
        return ApiResult.success("操作成功", scheduledTaskManageService.updateStatus(operatorId, taskId, body));
    }
}