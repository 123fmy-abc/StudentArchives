package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.dto.Fmy.form.request.FormTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.form.request.FormTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.form.response.FormTemplateResponse;
import com.example.studentarchives.service.Fmy.AdminFormTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * 管理端表单模板控制器
 * <p>
 * 对应图片“管理员 → 表单自定义 → 添加菜单/输入增加项目”。
 * 提供表单模板的 CRUD 与发布能力；
 * “发布信息”请使用 {@link AdminAnnouncementController}。
 */
@Slf4j
@RestController
@RequestMapping("/admin/form-templates")
@RequiredArgsConstructor
public class AdminFormTemplateController {

    private final AdminFormTemplateService adminFormTemplateService;

    @GetMapping
    public ApiResult<List<FormTemplateResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam("schoolId") Long schoolId) {
        return ApiResult.success(adminFormTemplateService.list(userId, schoolId));
    }

    @GetMapping("/{templateId}")
    public ApiResult<FormTemplateResponse> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        return ApiResult.success(adminFormTemplateService.detail(userId, templateId));
    }

    @AuditLog(module = "form_template", action = "create",
            description = "创建表单模板: #request.code", logResult = true)
    @PostMapping
    public ApiResult<FormTemplateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FormTemplateCreateRequest request) {
        return ApiResult.success("创建成功", adminFormTemplateService.create(userId, request));
    }

    @AuditLog(module = "form_template", action = "update",
            description = "更新表单模板: #templateId", logResult = true)
    @PutMapping("/{templateId}")
    public ApiResult<FormTemplateResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId,
            @Valid @RequestBody FormTemplateUpdateRequest request) {
        return ApiResult.success("更新成功", adminFormTemplateService.update(userId, templateId, request));
    }

    /**
     * 发布表单模板（递增版本号）
     * <p>
     * 注意：此处的“发布”指表单模板版本生效；向用户发布公告请使用
     * POST /admin/announcements。
     */
    @AuditLog(module = "form_template", action = "publish",
            description = "发布表单模板: #templateId", logResult = true)
    @PostMapping("/{templateId}/publish")
    public ApiResult<FormTemplateResponse> publish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        return ApiResult.success("发布成功", adminFormTemplateService.publish(userId, templateId));
    }
}
