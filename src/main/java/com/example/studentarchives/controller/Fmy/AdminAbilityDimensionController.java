package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.abilitydimension.request.AbilityDimensionCreateRequest;
import com.example.studentarchives.dto.Fmy.abilitydimension.request.AbilityDimensionUpdateRequest;
import com.example.studentarchives.dto.Fmy.abilitydimension.response.AbilityDimensionResponse;
import com.example.studentarchives.service.Fmy.AdminAbilityDimensionService;
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
 * 管理端能力维度配置控制器
 * <p>
 * 对应《管理端接口文档》能力维度管理模块，统一前缀 /admin/ability-dimensions。
 * 所有接口需通过 {@link com.example.studentarchives.service.Fmy.AdminAbilityDimensionService}
 * 校验 admin 角色或 indicator:manage 权限码，越权返回 20005 无访问权限。
 * 写操作写入 audit_log 审计日志（module=ability_dimension）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/ability-dimensions")
@RequiredArgsConstructor
public class AdminAbilityDimensionController {

    private final AdminAbilityDimensionService adminAbilityDimensionService;

    /**
     * 获取能力维度列表
     */
    @GetMapping
    public ApiResult<List<AbilityDimensionResponse>> list(@AuthenticationPrincipal Long userId) {
        List<AbilityDimensionResponse> list = adminAbilityDimensionService.list(userId);
        return ApiResult.success(list);
    }

    /**
     * 创建能力维度
     */
    @AuditLog(module = "ability_dimension", action = "create",
            description = "创建能力维度: #request.dimensionCode", logResult = true)
    @PostMapping
    public ApiResult<AbilityDimensionResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AbilityDimensionCreateRequest request) {
        AbilityDimensionResponse response = adminAbilityDimensionService.create(userId, request);
        return ApiResult.success("创建成功", response);
    }

    /**
     * 更新能力维度
     */
    @AuditLog(module = "ability_dimension", action = "update",
            description = "更新能力维度: #id", logResult = true)
    @PutMapping("/{id}")
    public ApiResult<AbilityDimensionResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AbilityDimensionUpdateRequest request) {
        AbilityDimensionResponse response = adminAbilityDimensionService.update(userId, id, request);
        return ApiResult.success("更新成功", response);
    }

    /**
     * 删除能力维度（软删除）
     */
    @AuditLog(module = "ability_dimension", action = "delete", description = "删除能力维度: #id")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        adminAbilityDimensionService.delete(userId, id);
        return ApiResult.success("删除成功", null);
    }
}
