package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorCreateRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorPublishRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorRuleVersionSnapshotPatchRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorStatusBatchRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorStatusUpdateRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorUpdateRequest;
import com.example.studentarchives.dto.Fmy.indicator.response.AdminIndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorCreateResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorPublishResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorRuleVersionItem;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorStatusChangeResponse;
import com.example.studentarchives.service.Fmy.AdminIndicatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端指标配置控制器
 * <p>
 * 对应《管理端接口文档》一、指标配置模块（1.1~1.9），统一前缀 /admin/indicators。
 * 所有接口需通过 {@link com.example.studentarchives.service.Fmy.AdminIndicatorService}
 * 校验 admin 角色或 indicator:manage 权限码，越权返回 20005 无访问权限。
 * 写操作（创建/更新/删除/发布/状态变更）写入 audit_log 审计日志（module=indicator）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/indicators")
@RequiredArgsConstructor
public class AdminIndicatorController {

    private final AdminIndicatorService adminIndicatorService;

    // ==================== 1.1 获取指标树 ====================

    /**
     * 获取指标树
     * <p>
     * 返回当前登录用户所属学校下完整的三级指标树，节点携带权重、状态、计分规则与当前版本信息。
     * 按学期过滤：semesterId 不传取当前学期；该学期已发布过规则版本则返回其权威快照。
     * 未发布过规则版本时：
     * <ul>
     *   <li>{@code draft=true} → 返回当前草稿树（含未发布的草稿改动，供发布前编辑确认）；</li>
     *   <li>{@code draft=false}（默认）→ 回退到全校当前生效/最新已发布版本的权威快照，
     *       避免管理端在指标体系重组期间看到半成品草稿。</li>
     * </ul>
     * 可按 status 过滤（0=禁用 1=启用）。
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param status     0=禁用 1=启用，不传返回全部
     * @param draft      true=强制返回当前草稿树；false/null=优先返回已发布版本的权威快照
     * @return 指标树
     */
    @GetMapping("/tree")
    public ApiResult<AdminIndicatorTreeResponse> getTree(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "draft", required = false) Boolean draft) {
        AdminIndicatorTreeResponse response = adminIndicatorService.getTree(userId, semesterId, status, draft);
        return ApiResult.success(response);
    }

    // ==================== 1.2 创建指标 ====================

    /**
     * 创建指标
     * <p>
     * 创建一级/二级/三级指标。创建三级指标时必须填写 scoringRule。
     * 指标编码学校内唯一；同级指标权重之和校验：一级之和=1，子级之和=父权重。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @AuditLog(module = "indicator", action = "create",
            description = "创建指标: #request.indicatorCode", logResult = true)
    @PostMapping
    public ApiResult<IndicatorCreateResponse> createIndicator(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody IndicatorCreateRequest request) {
        IndicatorCreateResponse response = adminIndicatorService.createIndicator(userId, request);
        return ApiResult.success("创建成功", response);
    }

    // ==================== 1.3 更新指标 ====================

    /**
     * 更新指标
     * <p>
     * 可更新名称、权重、说明、计分规则（仅三级）、排序。状态变更请使用 1.8/1.9 专用接口。
     * 修改 weight/scoringRule 仅作用于当前草稿树，不影响历史评分（发布时形成新版本快照）。
     *
     * @param userId      当前登录用户 ID
     * @param indicatorId 指标 ID
     * @param request     更新请求
     * @return 操作结果
     */
    @AuditLog(module = "indicator", action = "update",
            description = "更新指标: #indicatorId", logResult = true)
    @PutMapping("/{indicatorId}")
    public ApiResult<Void> updateIndicator(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long indicatorId,
            @Valid @RequestBody IndicatorUpdateRequest request) {
        adminIndicatorService.updateIndicator(userId, indicatorId, request);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 1.8 修改指标状态（单个） ====================

    /**
     * 修改指标状态（单个）
     * <p>
     * 单独变更某指标启用/禁用状态，与通用更新接口（1.3）解耦。
     * 禁用时自动级联禁用其所有后代节点；启用时校验启用同级权重之和不超过父权重（一级之和不超过 1），
     * 严格"等于父权重"由发布（1.5）统一校验。仅作用于当前草稿树，不影响已发布版本快照。
     *
     * @param userId      当前登录用户 ID
     * @param indicatorId 指标 ID
     * @param request     状态变更请求（status：0=禁用 1=启用）
     * @return 状态变更结果（含实际影响数量与级联禁用后代数）
     */
    @AuditLog(module = "indicator", action = "update-status",
            description = "修改指标状态: #indicatorId → status=#request.status", logResult = true)
    @PatchMapping("/{indicatorId}/status")
    public ApiResult<IndicatorStatusChangeResponse> updateIndicatorStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long indicatorId,
            @Valid @RequestBody IndicatorStatusUpdateRequest request) {
        IndicatorStatusChangeResponse response = adminIndicatorService.updateIndicatorStatus(userId, indicatorId, request);
        return ApiResult.success("修改成功", response);
    }

    // ==================== 1.9 批量修改指标状态 ====================

    /**
     * 批量修改指标状态
     * <p>
     * 对一批指标统一启用/禁用。整体一个事务，任一指标校验失败（指标不存在、跨学校混合、
     * 启用后同级权重超过父权重）则整批不生效（fail-fast）。禁用时对列表内每个指标级联禁用其所有后代。
     *
     * @param userId  当前登录用户 ID
     * @param request 批量状态变更请求（indicatorIds：1~100 个；status：0=禁用 1=启用）
     * @return 状态变更结果（含实际影响数量与级联禁用后代数）
     */
    @AuditLog(module = "indicator", action = "batch-status",
            description = "批量修改指标状态: status=#request.status, ids=#request.indicatorIds", logResult = true)
    @PatchMapping("/status")
    public ApiResult<IndicatorStatusChangeResponse> updateIndicatorStatusBatch(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody IndicatorStatusBatchRequest request) {
        IndicatorStatusChangeResponse response = adminIndicatorService.updateIndicatorStatusBatch(userId, request);
        return ApiResult.success("批量修改成功", response);
    }

    // ==================== 1.4 删除指标 ====================

    /**
     * 删除指标（软删除）
     * <p>
     * 存在子指标时返回 41003 存在子指标。
     *
     * @param userId      当前登录用户 ID
     * @param indicatorId 指标 ID
     * @return 操作结果
     */
    @AuditLog(module = "indicator", action = "delete", description = "删除指标: #indicatorId")
    @DeleteMapping("/{indicatorId}")
    public ApiResult<Void> deleteIndicator(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long indicatorId) {
        adminIndicatorService.deleteIndicator(userId, indicatorId);
        return ApiResult.success("删除成功", null);
    }

    // ==================== 1.5 发布指标规则版本 ====================

    /**
     * 发布指标规则版本
     * <p>
     * 将当前指标配置打包发布为一个新的规则版本，后续评分计算均使用本次发布版本，历史评分不受影响。
     * 发布版本可指定归属学期（semesterId，不传则取该校当前学期），供 GET /admin/indicators/tree 按学期过滤。
     * 若指定 sourceVersionId，则基于该历史版本的快照深拷贝发布，而不是从当前草稿树发布，
     * 避免连带引入草稿中其他未预期改动。
     *
     * @param userId  当前登录用户 ID
     * @param request 发布请求
     * @return 发布结果（新版本号）
     */
    @AuditLog(module = "indicator", action = "publish",
            description = "发布指标规则版本: #request.versionName", logResult = true)
    @PostMapping("/publish")
    public ApiResult<IndicatorPublishResponse> publish(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody IndicatorPublishRequest request) {
        IndicatorPublishResponse response = adminIndicatorService.publish(userId, request);
        return ApiResult.success("发布成功", response);
    }

    // ==================== 1.6 指标规则版本列表 ====================

    /**
     * 获取指标规则版本列表
     * <p>
     * 分页查询当前登录用户所属学校下历史发布的规则版本，按版本号倒序。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，预留参数）
     * @param page       页码，默认 1
     * @param perPage    每页条数，默认 20，最大 100
     * @return 分页的规则版本列表
     */
    @GetMapping("/rule-versions")
    public ApiResult<PageResult<IndicatorRuleVersionItem>> listRuleVersions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<IndicatorRuleVersionItem> result =
                adminIndicatorService.listRuleVersions(userId, semesterId, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 1.7 修补历史版本快照 ====================

    /**
     * 修补历史指标规则版本快照
     * <p>
     * 仅允许修改指定历史版本快照中某指标的元数据字段（indicatorName、description、indicatorCode），
     * 用于修正发布后发现的名字/说明/编码笔误。禁止修改 weight、scoringRule、status 等会影响评分或树结构的字段。
     *
     * @param userId    当前登录用户 ID
     * @param versionId 规则版本 ID
     * @param request   修补请求
     * @return 操作结果
     */
    @AuditLog(module = "indicator", action = "patch-snapshot",
            description = "修补指标规则版本快照: versionId=#versionId, indicatorCode=#request.indicatorCode", logResult = true)
    @PatchMapping("/rule-versions/{versionId}/snapshot")
    public ApiResult<Void> patchRuleVersionSnapshot(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long versionId,
            @Valid @RequestBody IndicatorRuleVersionSnapshotPatchRequest request) {
        adminIndicatorService.patchRuleVersionSnapshot(userId, versionId, request);
        return ApiResult.success("修补成功", null);
    }
}
