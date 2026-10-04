package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskCreateRequest;
import com.example.studentarchives.service.schedule.ScheduledTaskHandlerRegistry;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskDeleteResponse;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskDetailResponse;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskItem;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskStatusResponse;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskStatusUpdateRequest;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskTriggerResponse;
import com.example.studentarchives.service.Lzw.ScheduledTaskManageService.ScheduledTaskUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端定时任务管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》十四、定时任务管理模块（14.1 ~ 14.7）。
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

    // ==================== 14.3 创建自定义定时任务 ====================

    @AuditLog(module = "scheduled-task", action = "create", description = "创建定时任务: #body.taskCode", relatedType = "scheduled-task")
    @PostMapping("/scheduled-tasks")
    public ApiResult<ScheduledTaskDetailResponse> createTask(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody ScheduledTaskCreateRequest body) {
        return ApiResult.success("创建成功", scheduledTaskManageService.createTask(operatorId, body));
    }

    // ==================== 14.3.1 获取可用处理器列表 ====================

    @GetMapping("/scheduled-tasks/handlers")
    public ApiResult<List<ScheduledTaskHandlerRegistry.HandlerMeta>> listHandlers() {
        return ApiResult.success(scheduledTaskManageService.listHandlers());
    }

    // ==================== 14.4 获取任务详情 ====================

    @GetMapping("/scheduled-tasks/{taskId}")
    public ApiResult<ScheduledTaskDetailResponse> getTaskDetail(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long taskId) {
        return ApiResult.success(scheduledTaskManageService.getTaskDetail(operatorId, taskId));
    }

    // ==================== 14.5 更新任务配置 ====================

    @AuditLog(module = "scheduled-task", action = "update", description = "更新定时任务: #taskId", relatedType = "scheduled-task", relatedId = "#taskId")
    @PutMapping("/scheduled-tasks/{taskId}")
    public ApiResult<ScheduledTaskDetailResponse> updateTask(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long taskId,
            @RequestBody ScheduledTaskUpdateRequest body) {
        return ApiResult.success("更新成功", scheduledTaskManageService.updateTask(operatorId, taskId, body));
    }

    // ==================== 14.6 删除自定义任务 ====================

    @AuditLog(module = "scheduled-task", action = "delete", description = "删除定时任务: #taskId", relatedType = "scheduled-task", relatedId = "#taskId")
    @DeleteMapping("/scheduled-tasks/{taskId}")
    public ApiResult<ScheduledTaskDeleteResponse> deleteTask(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long taskId) {
        return ApiResult.success("删除成功", scheduledTaskManageService.deleteTask(operatorId, taskId));
    }

    // ==================== 14.7 手动立即触发执行 ====================

    @AuditLog(module = "scheduled-task", action = "trigger", description = "手动触发定时任务: #taskId", relatedType = "scheduled-task", relatedId = "#taskId")
    @PostMapping("/scheduled-tasks/{taskId}/trigger")
    public ApiResult<ScheduledTaskTriggerResponse> triggerTask(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> runParams = body == null ? null : (Map<String, Object>) body.get("runParams");
        return ApiResult.success("任务已触发", scheduledTaskManageService.triggerTask(operatorId, taskId, runParams));
    }
}