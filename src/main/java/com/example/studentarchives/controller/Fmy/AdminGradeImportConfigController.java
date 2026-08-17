package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigSaveRequest;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigStatusRequest;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigUpdateRequest;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportConfigResponse;
import com.example.studentarchives.service.Fmy.AdminGradeImportConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端成绩导入配置控制器
 * <p>
 * 统一前缀 /admin/grade-import-configs，提供成绩导入配置的 CRUD 与状态管理。
 * 模板下载统一由 {@link AdminGradeController#importTemplate} 提供。
 * 所有接口需校验 admin 角色或 grade:import 权限码。
 */
@RestController
@RequestMapping("/admin/grade-import-configs")
@RequiredArgsConstructor
public class AdminGradeImportConfigController {

    private final AdminGradeImportConfigService adminGradeImportConfigService;

    /**
     * 获取当前学校成绩导入配置（GET /admin/grade-import-configs）
     */
    @GetMapping
    public ApiResult<GradeImportConfigResponse> getConfig(@AuthenticationPrincipal Long userId) {
        return ApiResult.success(adminGradeImportConfigService.getConfig(userId));
    }

    /**
     * 创建成绩导入配置（POST /admin/grade-import-configs）
     */
    @AuditLog(module = "grade-import-config", action = "create",
            description = "创建成绩导入配置", logResult = true)
    @PostMapping
    public ApiResult<GradeImportConfigResponse> createConfig(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GradeImportConfigSaveRequest request) {
        return ApiResult.success("创建成功", adminGradeImportConfigService.createConfig(userId, request));
    }

    /**
     * 更新成绩导入配置（PUT /admin/grade-import-configs/{id}）
     */
    @AuditLog(module = "grade-import-config", action = "update",
            description = "更新成绩导入配置: #id", logResult = true)
    @PutMapping("/{id}")
    public ApiResult<GradeImportConfigResponse> updateConfig(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody GradeImportConfigUpdateRequest request) {
        return ApiResult.success("更新成功", adminGradeImportConfigService.updateConfig(userId, id, request));
    }

    /**
     * 删除成绩导入配置（DELETE /admin/grade-import-configs/{id}）
     */
    @AuditLog(module = "grade-import-config", action = "delete",
            description = "删除成绩导入配置: #id", logResult = true)
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteConfig(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        adminGradeImportConfigService.deleteConfig(userId, id);
        return ApiResult.success("删除成功", null);
    }

    /**
     * 修改成绩导入配置状态（PATCH /admin/grade-import-configs/{id}/status）
     */
    @AuditLog(module = "grade-import-config", action = "update-status",
            description = "修改成绩导入配置状态: #id -> #request.status", logResult = true)
    @PatchMapping("/{id}/status")
    public ApiResult<GradeImportConfigResponse> updateStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody GradeImportConfigStatusRequest request) {
        return ApiResult.success("状态修改成功",
                adminGradeImportConfigService.updateStatus(userId, id, request.getStatus()));
    }
}
