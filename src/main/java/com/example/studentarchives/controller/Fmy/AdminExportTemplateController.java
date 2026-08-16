package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.export.request.ExportTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.export.request.ExportTemplateStatusRequest;
import com.example.studentarchives.dto.Fmy.export.request.ExportTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateCreateResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDefaultResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDeleteResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateDetailResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateItem;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplatePreviewImageResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateStatusResponse;
import com.example.studentarchives.dto.Fmy.export.response.ExportTemplateUpdateResponse;
import com.example.studentarchives.service.Fmy.AdminExportTemplateService;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端导出模板控制器
 * <p>
 * 对应《管理端接口文档》五、数据导出模块（5.3~5.10），统一前缀 /admin/export-templates。
 * 所有接口需校验 admin 角色或 export:template:manage 权限码，越权返回 20005 无访问权限。
 * 写操作（创建/更新/修改状态/删除/设置默认）写入 audit_log 审计日志（module=export-template）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/export-templates")
@RequiredArgsConstructor
public class AdminExportTemplateController {

    private final AdminExportTemplateService adminExportTemplateService;

    // ==================== 5.3 获取导出模板列表 ====================

    /**
     * 获取导出模板列表（GET /admin/export-templates，文档 5.3）
     * <p>
     * 查询当前登录用户所属学校维度的导出模板列表，支持按导出类型和启用状态筛选，按更新时间倒序分页。
     *
     * @param userId     当前登录用户 ID
     * @param exportType 导出类型（可选）：student_archive / career_plan / resume
     * @param status     0=禁用 1=启用（可选，不传返回全部）
     * @param page       页码，默认 1
     * @param perPage    每页条数，默认 20，最大 100
     * @return 分页的模板列表
     */
    @GetMapping
    public ApiResult<PageResult<ExportTemplateItem>> listTemplates(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "exportType", required = false) String exportType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<ExportTemplateItem> result =
                adminExportTemplateService.listTemplates(userId, exportType, status, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 5.4 获取导出模板详情 ====================

    /**
     * 获取导出模板详情（GET /admin/export-templates/{templateId}，文档 5.4）
     * <p>
     * 返回模板完整配置，包括模板内容、字段配置、页眉页脚、水印与字体配置等。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 模板完整配置
     */
    @GetMapping("/{templateId}")
    public ApiResult<ExportTemplateDetailResponse> getTemplateDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        ExportTemplateDetailResponse response = adminExportTemplateService.getTemplateDetail(userId, templateId);
        return ApiResult.success(response);
    }

    // ==================== 5.5 创建导出模板 ====================

    /**
     * 创建导出模板（POST /admin/export-templates，文档 5.5）
     * <p>
     * 创建学校维度的自定义导出模板，version 初始 1，is_default=0；模板编码同校内唯一。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @AuditLog(module = "export-template", action = "create",
            description = "创建导出模板: #request.templateCode", logResult = true)
    @PostMapping
    public ApiResult<ExportTemplateCreateResponse> createTemplate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExportTemplateCreateRequest request) {
        ExportTemplateCreateResponse response = adminExportTemplateService.createTemplate(userId, request);
        return ApiResult.success("创建成功", response);
    }

    // ==================== 5.6 更新导出模板 ====================

    /**
     * 更新导出模板（PUT /admin/export-templates/{templateId}，文档 5.6）
     * <p>
     * 全部字段可选，未传表示不修改；更新成功时 version 自动 +1。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param request    更新请求
     * @return 更新结果
     */
    @AuditLog(module = "export-template", action = "update",
            description = "更新导出模板: #templateId", logResult = true)
    @PutMapping("/{templateId}")
    public ApiResult<ExportTemplateUpdateResponse> updateTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId,
            @Valid @RequestBody ExportTemplateUpdateRequest request) {
        ExportTemplateUpdateResponse response = adminExportTemplateService.updateTemplate(userId, templateId, request);
        return ApiResult.success("更新成功", response);
    }

    // ==================== 5.7 删除导出模板 ====================

    /**
     * 删除导出模板（DELETE /admin/export-templates/{templateId}，文档 5.7）
     * <p>
     * 软删除；当前为默认模板（is_default=1）时不允许删除。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 删除结果
     */
    @AuditLog(module = "export-template", action = "delete",
            description = "删除导出模板: #templateId", logResult = true)
    @DeleteMapping("/{templateId}")
    public ApiResult<ExportTemplateDeleteResponse> deleteTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        ExportTemplateDeleteResponse response = adminExportTemplateService.deleteTemplate(userId, templateId);
        return ApiResult.success("删除成功", response);
    }

    // ==================== 5.8 设置默认导出模板 ====================

    /**
     * 设置默认导出模板（PUT /admin/export-templates/{templateId}/default，文档 5.8）
     * <p>
     * 将指定模板设为某学校、某导出类型下的默认模板，同时取消同校同类型其他模板的默认状态。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 设置结果
     */
    @AuditLog(module = "export-template", action = "set-default",
            description = "设置默认导出模板: #templateId", logResult = true)
    @PutMapping("/{templateId}/default")
    public ApiResult<ExportTemplateDefaultResponse> setDefaultTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        ExportTemplateDefaultResponse response = adminExportTemplateService.setDefaultTemplate(userId, templateId);
        return ApiResult.success("设置成功", response);
    }

    // ==================== 5.9 修改导出模板状态 ====================

    /**
     * 修改导出模板状态（PATCH /admin/export-templates/{templateId}/status，文档 5.9）
     * <p>
     * 仅变更启用/禁用状态，不触发 version 自增；禁用默认模板（is_default=1）不允许。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param request    状态请求（仅 status 字段）
     * @return 修改后的状态
     */
    @AuditLog(module = "export-template", action = "update-status",
            description = "修改导出模板状态: #templateId -> #request.status", logResult = true)
    @PatchMapping("/{templateId}/status")
    public ApiResult<ExportTemplateStatusResponse> updateTemplateStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId,
            @Valid @RequestBody ExportTemplateStatusRequest request) {
        ExportTemplateStatusResponse response =
                adminExportTemplateService.updateTemplateStatus(userId, templateId, request.getStatus());
        return ApiResult.success("状态修改成功", response);
    }

    // ==================== 5.10 上传导出模板预览图 ====================

    /**
     * 上传导出模板预览图（POST /admin/export-templates/{templateId}/preview-image，文档 5.10）
     * <p>
     * 上传图片并回写模板 preview_image 字段；再次调用即覆盖原预览图（upsert），
     * 不触发 version 自增（预览图仅供管理端展示，导出任务渲染不消费）。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param file       预览图文件（jpg/jpeg/png/gif/webp/bmp，≤2MB）
     * @return 上传结果
     */
    @AuditLog(module = "export-template", action = "upload-preview",
            description = "上传导出模板预览图: #templateId", logResult = true)
    @PostMapping("/{templateId}/preview-image")
    public ApiResult<ExportTemplatePreviewImageResponse> uploadPreviewImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file) {
        ExportTemplatePreviewImageResponse response =
                adminExportTemplateService.uploadPreviewImage(userId, templateId, file);
        return ApiResult.success("上传成功", response);
    }
}
