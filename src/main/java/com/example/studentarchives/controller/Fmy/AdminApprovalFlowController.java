package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowCreateRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowMappingUpsertRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowStepsRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.request.ApprovalFlowUpdateRequest;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowCreateResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowDeleteResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowDetailResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowItem;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingDeleteResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingItem;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowMappingResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowStepResponse;
import com.example.studentarchives.dto.Fmy.approvalflow.response.ApprovalFlowStepsResponse;
import com.example.studentarchives.service.Fmy.AdminApprovalFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 管理端审批流程配置控制器
 * <p>
 * 对应《管理端接口文档》六、审批流程配置模块，统一前缀 /admin/approval-flows 与
 * /admin/approval-flow-mappings。所有接口需校验 admin 角色或 approval:flow:manage 权限码，
 * 越权返回 20005 无访问权限。写操作写入 audit_log 审计日志（module=approval-flow）。
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApprovalFlowController {

    private final AdminApprovalFlowService adminApprovalFlowService;

    // ==================== 6.1 获取审批流程列表 ====================

    /**
     * 获取审批流程列表（GET /admin/approval-flows，文档 6.1）
     * <p>
     * 按当前登录用户所属学校、适用类型、启用状态筛选，按 id 倒序分页。
     *
     * @param userId          当前登录用户 ID
     * @param applicableType  适用类型（可选）：Archive/AwardApplication/CareerPlan/GrowthTimeline/Announcement
     * @param status          0=禁用 1=启用（可选，不传返回全部）
     * @param page            页码，默认 1
     * @param perPage         每页条数，默认 20
     * @return 分页的流程列表
     */
    @GetMapping("/approval-flows")
    public ApiResult<PageResult<ApprovalFlowItem>> listFlows(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "applicableType", required = false) String applicableType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<ApprovalFlowItem> result =
                adminApprovalFlowService.listFlows(userId, applicableType, status, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 6.2 创建审批流程 ====================

    /**
     * 创建审批流程（POST /admin/approval-flows，文档 6.2）
     * <p>
     * version 初始 1；可选携带初始步骤列表（结构见 6.7 步骤项字段）。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @AuditLog(module = "approval-flow", action = "create",
            description = "创建审批流程: #request.flowName", logResult = true, relatedType = "approval_flow")
    @PostMapping("/approval-flows")
    public ApiResult<ApprovalFlowCreateResponse> createFlow(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ApprovalFlowCreateRequest request) {
        ApprovalFlowCreateResponse response = adminApprovalFlowService.createFlow(userId, request);
        return ApiResult.success("创建成功", response);
    }

    // ==================== 6.3 获取审批流程详情 ====================

    /**
     * 获取审批流程详情（GET /admin/approval-flows/{flowId}，文档 6.3）
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 流程详情（含步骤列表）
     */
    @GetMapping("/approval-flows/{flowId}")
    public ApiResult<ApprovalFlowDetailResponse> getFlowDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long flowId) {
        ApprovalFlowDetailResponse response = adminApprovalFlowService.getFlowDetail(userId, flowId);
        return ApiResult.success(response);
    }

    // ==================== 6.4 更新审批流程 ====================

    /**
     * 更新审批流程（PUT /admin/approval-flows/{flowId}，文档 6.4）
     * <p>
     * 仅更新流程基础信息；步骤维护使用 6.7。已存在审批实例引用的流程禁止修改适用类型/子类型。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @param request 更新请求（全字段可选，null=不修改）
     * @return 更新结果
     */
    @AuditLog(module = "approval-flow", action = "update",
            description = "更新审批流程: #flowId", logResult = true, relatedType = "approval_flow", relatedId = "#flowId")
    @PutMapping("/approval-flows/{flowId}")
    public ApiResult<ApprovalFlowCreateResponse> updateFlow(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long flowId,
            @Valid @RequestBody ApprovalFlowUpdateRequest request) {
        ApprovalFlowCreateResponse response = adminApprovalFlowService.updateFlow(userId, flowId, request);
        return ApiResult.success("更新成功", response);
    }

    // ==================== 6.5 删除审批流程 ====================

    /**
     * 删除审批流程（DELETE /admin/approval-flows/{flowId}，文档 6.5）
     * <p>
     * 软删除；存在进行中的审批实例时禁止删除。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 删除结果
     */
    @AuditLog(module = "approval-flow", action = "delete",
            description = "删除审批流程: #flowId", relatedType = "approval_flow", relatedId = "#flowId")
    @DeleteMapping("/approval-flows/{flowId}")
    public ApiResult<ApprovalFlowDeleteResponse> deleteFlow(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long flowId) {
        ApprovalFlowDeleteResponse response = adminApprovalFlowService.deleteFlow(userId, flowId);
        return ApiResult.success("删除成功", response);
    }

    // ==================== 6.6 获取流程步骤列表 ====================

    /**
     * 获取流程步骤列表（GET /admin/approval-flows/{flowId}/steps，文档 6.6）
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @return 步骤列表（按 stepNo 升序）
     */
    @GetMapping("/approval-flows/{flowId}/steps")
    public ApiResult<List<ApprovalFlowStepResponse>> listSteps(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long flowId) {
        List<ApprovalFlowStepResponse> response = adminApprovalFlowService.listSteps(userId, flowId);
        return ApiResult.success(response);
    }

    // ==================== 6.7 保存流程步骤 ====================

    /**
     * 保存流程步骤（PUT /admin/approval-flows/{flowId}/steps，文档 6.7）
     * <p>
     * 完整步骤列表全量覆盖（按 step_no 匹配，未包含的旧步骤软删除）。
     *
     * @param userId  当前登录用户 ID
     * @param flowId  流程 ID
     * @param request 步骤保存请求（steps 必填）
     * @return 保存后的步骤列表
     */
    @AuditLog(module = "approval-flow", action = "save-steps",
            description = "保存审批流程步骤: #flowId", logResult = true, relatedType = "approval_flow", relatedId = "#flowId")
    @PutMapping("/approval-flows/{flowId}/steps")
    public ApiResult<ApprovalFlowStepsResponse> saveSteps(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long flowId,
            @Valid @RequestBody ApprovalFlowStepsRequest request) {
        ApprovalFlowStepsResponse response = adminApprovalFlowService.saveSteps(userId, flowId, request);
        return ApiResult.success("保存成功", response);
    }

    // ==================== 6.8 获取审批流程映射列表 ====================

    /**
     * 获取审批流程映射列表（GET /admin/approval-flow-mappings，文档 6.8）
     * <p>
     * 按当前登录用户所属学校、业务类型、业务子类型筛选，按 id 倒序分页；响应冗余回显流程名称。
     *
     * @param userId          当前登录用户 ID
     * @param businessType    业务类型（可选）
     * @param businessSubType 业务子类型（可选）
     * @param page            页码，默认 1
     * @param perPage         每页条数，默认 20
     * @return 分页的映射列表
     */
    @GetMapping("/approval-flow-mappings")
    public ApiResult<PageResult<ApprovalFlowMappingItem>> listMappings(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessSubType", required = false) String businessSubType,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<ApprovalFlowMappingItem> result = adminApprovalFlowService
                .listMappings(userId, businessType, businessSubType, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 6.9 创建/更新审批流程映射 ====================

    /**
     * 创建/更新审批流程映射（POST /admin/approval-flow-mappings，文档 6.9）
     * <p>
     * 若传入 id 则更新，否则创建。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建/更新请求
     * @return 映射响应
     */
    @AuditLog(module = "approval-flow", action = "upsert-mapping",
            description = "创建/更新审批流程映射: businessType=#request.businessType, flowId=#request.flowId",
            logResult = true, relatedType = "approval_flow_mapping")
    @PostMapping("/approval-flow-mappings")
    public ApiResult<ApprovalFlowMappingResponse> upsertMapping(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ApprovalFlowMappingUpsertRequest request) {
        ApprovalFlowMappingResponse response = adminApprovalFlowService.upsertMapping(userId, request);
        return ApiResult.success(request.getId() != null ? "更新成功" : "创建成功", response);
    }

    // ==================== 6.10 删除审批流程映射 ====================

    /**
     * 删除审批流程映射（DELETE /admin/approval-flow-mappings/{mappingId}，文档 6.10）
     * <p>
     * 软删除。
     *
     * @param userId    当前登录用户 ID
     * @param mappingId 映射 ID
     * @return 删除结果
     */
    @AuditLog(module = "approval-flow", action = "delete-mapping",
            description = "删除审批流程映射: #mappingId", relatedType = "approval_flow_mapping", relatedId = "#mappingId")
    @DeleteMapping("/approval-flow-mappings/{mappingId}")
    public ApiResult<ApprovalFlowMappingDeleteResponse> deleteMapping(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mappingId) {
        ApprovalFlowMappingDeleteResponse response = adminApprovalFlowService.deleteMapping(userId, mappingId);
        return ApiResult.success("删除成功", response);
    }
}
