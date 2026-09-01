package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.AuditTaskService;
import com.example.studentarchives.service.Lzw.AuditTaskService.ApproveRequest;
import com.example.studentarchives.service.Lzw.AuditTaskService.ApproveResult;
import com.example.studentarchives.service.Lzw.AuditTaskService.BatchApproveRequest;
import com.example.studentarchives.service.Lzw.AuditTaskService.BatchResult;
import com.example.studentarchives.service.Lzw.AuditTaskService.PendingDetailResponse;
import com.example.studentarchives.service.Lzw.AuditTaskService.PendingListItem;
import com.example.studentarchives.service.Lzw.AuditTaskService.RejectRequest;
import com.example.studentarchives.service.Lzw.AuditTaskService.RejectResult;
import com.example.studentarchives.service.Lzw.AuditTaskService.RejectTemplateItem;
import com.example.studentarchives.service.Lzw.AuditTaskService.RevokeRequest;
import com.example.studentarchives.service.Lzw.AuditTaskService.RevokeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端「待审核任务模块」控制器（Lzw）
 * <p>
 * 对应《教师端接口文档》四、待审核任务模块（4.1 ~ 4.7）。
 * 路径前缀 {@code /teacher/audits}，全部接口需认证；
 * 撤销接口（4.6）在 HTTP 层（SecurityConfig）已要求 admin 角色，Service 层再次校验。
 */
@RestController
@RequestMapping("/teacher/audits")
@RequiredArgsConstructor
public class TeacherAuditController {

    private final AuditTaskService auditTaskService;

    // ==================== 4.1 获取待审核列表 ====================

    @GetMapping("/pending")
    public ApiResult<PageResult<PendingListItem>> listPending(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "archiveType", required = false) String archiveType,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false, defaultValue = "submit_time") String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(auditTaskService.listPending(userId, type, archiveType, scopeType, scopeId,
                semesterId, grade, keyword, sortBy, sortOrder, pageParam));
    }

    // ==================== 4.2 获取待审核详情 ====================

    @GetMapping("/pending/{taskId}")
    public ApiResult<PendingDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "archiveType", required = false) String archiveType,
            @RequestParam(value = "scopeType", required = false) Integer scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "asc") String sortOrder) {
        return ApiResult.success(auditTaskService.getDetail(userId, taskId, type, archiveType, scopeType,
                scopeId, semesterId, grade, keyword, sortOrder));
    }

    // ==================== 4.3 单个审核通过 ====================

    @PostMapping("/{taskId}/approve")
    public ApiResult<ApproveResult> approve(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestBody ApproveRequest body) {
        return ApiResult.success("审核通过", auditTaskService.approve(userId, taskId,
                body.getComment(), body.getNextAuditorId()));
    }

    // ==================== 4.4 单个审核退回 ====================

    @PostMapping("/{taskId}/reject")
    public ApiResult<RejectResult> reject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestBody RejectRequest body) {
        return ApiResult.success("已退回", auditTaskService.reject(userId, taskId,
                body.getComment(), body.getTemplateCode(), body.getRejectToStep()));
    }

    // ==================== 4.5 批量审核通过 ====================

    @PostMapping("/batch/approve")
    public ApiResult<BatchResult> batchApprove(
            @AuthenticationPrincipal Long userId,
            @RequestBody BatchApproveRequest body) {
        return ApiResult.success("批量审核完成", auditTaskService.batchApprove(userId,
                body.getTaskIds(), body.getComment()));
    }

    // ==================== 4.6 撤销已审核记录 ====================

    @PostMapping("/{taskId}/revoke")
    public ApiResult<RevokeResult> revoke(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestBody RevokeRequest body) {
        return ApiResult.success("撤销成功", auditTaskService.revoke(userId, taskId,
                body.getRevokeReason()));
    }

    // ==================== 4.7 获取常用退回原因模板 ====================

    @GetMapping("/reject-templates")
    public ApiResult<List<RejectTemplateItem>> rejectTemplates(
            @AuthenticationPrincipal Long userId) {
        return ApiResult.success(auditTaskService.getRejectTemplates(userId));
    }
}