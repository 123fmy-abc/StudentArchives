package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.growthscorerule.request.GrowthScoreRuleUpsertRequest;
import com.example.studentarchives.dto.Fmy.growthscorerule.response.GrowthScoreRuleResponse;
import com.example.studentarchives.service.Fmy.AdminGrowthEventScoreRuleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端成长事件计分规则控制器
 * <p>
 * 统一前缀 /admin/growth-event-score-rules。每种事件类型（1-6）仅一条规则，
 * 学生新增/修改成长时间轴事件时，能力得分由规则按事件类型自动生成，学生不再自填分数。
 * 所有接口需通过 {@link AdminGrowthEventScoreRuleService} 校验 admin 角色或
 * indicator:manage 权限码，越权返回 20005 无访问权限。
 * 写操作写入 audit_log 审计日志（module=growth_event_score_rule）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/growth-event-score-rules")
@RequiredArgsConstructor
public class AdminGrowthEventScoreRuleController {

    private final AdminGrowthEventScoreRuleService adminGrowthEventScoreRuleService;

    /**
     * 获取计分规则列表
     */
    @GetMapping
    public ApiResult<List<GrowthScoreRuleResponse>> list(@AuthenticationPrincipal Long userId) {
        List<GrowthScoreRuleResponse> list = adminGrowthEventScoreRuleService.list(userId);
        return ApiResult.success(list);
    }

    /**
     * 创建计分规则（同一事件类型仅允许一条）
     */
    @AuditLog(module = "growth_event_score_rule", action = "create",
            description = "创建成长事件计分规则: #request.eventType", logResult = true)
    @PostMapping
    public ApiResult<GrowthScoreRuleResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GrowthScoreRuleUpsertRequest request) {
        GrowthScoreRuleResponse response = adminGrowthEventScoreRuleService.create(userId, request);
        return ApiResult.success("创建成功", response);
    }

    /**
     * 更新计分规则
     */
    @AuditLog(module = "growth_event_score_rule", action = "update",
            description = "更新成长事件计分规则: #id", logResult = true)
    @PutMapping("/{id}")
    public ApiResult<GrowthScoreRuleResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody GrowthScoreRuleUpsertRequest request) {
        GrowthScoreRuleResponse response = adminGrowthEventScoreRuleService.update(userId, id, request);
        return ApiResult.success("更新成功", response);
    }

    /**
     * 删除计分规则（软删除）
     */
    @AuditLog(module = "growth_event_score_rule", action = "delete", description = "删除成长事件计分规则: #id")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        adminGrowthEventScoreRuleService.delete(userId, id);
        return ApiResult.success("删除成功", null);
    }
}
