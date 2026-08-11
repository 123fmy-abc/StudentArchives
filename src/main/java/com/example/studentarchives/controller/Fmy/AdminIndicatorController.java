package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorCreateRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorPublishRequest;
import com.example.studentarchives.dto.Fmy.indicator.request.IndicatorUpdateRequest;
import com.example.studentarchives.dto.Fmy.indicator.response.AdminIndicatorTreeResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorCreateResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorPublishResponse;
import com.example.studentarchives.dto.Fmy.indicator.response.IndicatorRuleVersionItem;
import com.example.studentarchives.service.Fmy.AdminIndicatorService;
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

/**
 * 管理端指标配置控制器
 * <p>
 * 对应《管理端接口文档》一、指标配置模块（1.1~1.6），统一前缀 /admin/indicators。
 * 所有接口需通过 {@link com.example.studentarchives.service.Fmy.AdminIndicatorService}
 * 校验 admin 角色或 indicator:manage 权限码，越权返回 20005 无访问权限。
 * 写操作（创建/更新/删除/发布）写入 audit_log 审计日志（module=indicator）。
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
     * 返回学校下完整的三级指标树，节点携带权重、状态、计分规则与当前版本信息。
     * 按学期过滤：semesterId 不传取当前学期；该学期已发布过规则版本则返回其
     * 权威快照（只读历史视图），未发布过则返回当前草稿树（顶部带当前生效版本元数据）。
     * 可按 status 过滤（0=禁用 1=启用）。
     *
     * @param userId     当前登录用户 ID（由 JWT 过滤器注入）
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param status     0=禁用 1=启用，不传返回全部
     * @return 指标树
     */
    @GetMapping("/tree")
    public ApiResult<AdminIndicatorTreeResponse> getTree(
            @AuthenticationPrincipal Long userId,
            @RequestParam("schoolId") Long schoolId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "status", required = false) Integer status) {
        AdminIndicatorTreeResponse response = adminIndicatorService.getTree(userId, schoolId, semesterId, status);
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
     * 可更新名称、权重、说明、计分规则（仅三级）、排序、状态。
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
     * 将当前草稿指标树打包为新的规则版本：校验权重（一级之和=1、子级之和=父权重，失败返回 41004）、
     * 版本名称不可重复（重复返回 41002），发布后生成 indicator_versions 快照并全校推进版本号。
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
     * 分页查询学校下历史发布的规则版本，按版本号倒序。
     *
     * @param userId     当前登录用户 ID
     * @param schoolId   学校 ID
     * @param semesterId 学期 ID（可选，预留参数）
     * @param page       页码，默认 1
     * @param perPage    每页条数，默认 20，最大 100
     * @return 分页的规则版本列表
     */
    @GetMapping("/rule-versions")
    public ApiResult<PageResult<IndicatorRuleVersionItem>> listRuleVersions(
            @AuthenticationPrincipal Long userId,
            @RequestParam("schoolId") Long schoolId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<IndicatorRuleVersionItem> result =
                adminIndicatorService.listRuleVersions(userId, schoolId, semesterId, pageParam);
        return ApiResult.success(result);
    }
}
