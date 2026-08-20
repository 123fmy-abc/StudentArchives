package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.formtemplate.request.FormTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.formtemplate.request.FormTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateCreateResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDefaultResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDeleteResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDetailResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateItem;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateUpdateResponse;
import com.example.studentarchives.service.Fmy.AdminFormTemplateService;
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
 * 管理端表单自定义模板控制器
 * <p>
 * 对应《管理端接口文档》十七、表单自定义模板模块（17.1~17.6），统一前缀 /admin/form-templates。
 * 所有接口需校验 admin 角色或 form:template:manage 权限码，越权返回 20005 无访问权限。
 * 写操作（创建/更新/删除/设置默认）写入 audit_log 审计日志（module=form-template）。
 * 学校范围由当前登录用户推导，不接受前端传入 schoolId。
 */
@Slf4j
@RestController
@RequestMapping("/admin/form-templates")
@RequiredArgsConstructor
public class AdminFormTemplateController {

    private final AdminFormTemplateService adminFormTemplateService;

    // ==================== 17.1 获取表单模板列表 ====================

    /**
     * 获取表单模板列表（GET /admin/form-templates，文档 17.1）
     * <p>
     * 查询当前登录用户所属学校维度的表单模板列表，支持按适用类别、模板编码、
     * 启用状态、名称关键词筛选，按更新时间倒序分页。列表项不含 fields、layoutConfig 大字段。
     *
     * @param userId   当前登录用户 ID
     * @param category 适用类别（可选）：archive/award/career_plan
     * @param code     模板编码（可选）
     * @param status   0=禁用 1=启用（可选，不传返回全部）
     * @param keyword  模板名称模糊搜索（可选）
     * @param page     页码，默认 1
     * @param perPage  每页条数，默认 20，最大 100
     * @return 分页的模板列表
     */
    @GetMapping
    public ApiResult<PageResult<FormTemplateItem>> listTemplates(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        PageResult<FormTemplateItem> result =
                adminFormTemplateService.listTemplates(userId, category, code, status, keyword, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 17.2 获取表单模板详情 ====================

    /**
     * 获取表单模板详情（GET /admin/form-templates/{templateId}，文档 17.2）
     * <p>
     * 返回模板完整配置：字段配置 fields、布局配置 layoutConfig、适用角色与版本信息。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 模板完整配置
     */
    @GetMapping("/{templateId}")
    public ApiResult<FormTemplateDetailResponse> getTemplateDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        FormTemplateDetailResponse response = adminFormTemplateService.getTemplateDetail(userId, templateId);
        return ApiResult.success(response);
    }

    // ==================== 17.3 创建表单模板 ====================

    /**
     * 创建表单模板（POST /admin/form-templates，文档 17.3）
     * <p>
     * 创建学校维度的表单自定义模板，version 初始 1，is_default 默认 0；
     * 同一学校、同一 code + category 下唯一。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    @AuditLog(module = "form-template", action = "create",
            description = "创建表单模板: #request.code", logResult = true)
    @PostMapping
    public ApiResult<FormTemplateCreateResponse> createTemplate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FormTemplateCreateRequest request) {
        FormTemplateCreateResponse response = adminFormTemplateService.createTemplate(userId, request);
        return ApiResult.success("创建成功", response);
    }

    // ==================== 17.4 更新表单模板 ====================

    /**
     * 更新表单模板（PUT /admin/form-templates/{templateId}，文档 17.4）
     * <p>
     * 全部字段可选，未传表示不修改；更新成功时 version 自动 +1，历史申报按字段快照不受影响。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param request    更新请求
     * @return 更新结果
     */
    @AuditLog(module = "form-template", action = "update",
            description = "更新表单模板: #templateId", logResult = true)
    @PutMapping("/{templateId}")
    public ApiResult<FormTemplateUpdateResponse> updateTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId,
            @Valid @RequestBody FormTemplateUpdateRequest request) {
        FormTemplateUpdateResponse response = adminFormTemplateService.updateTemplate(userId, templateId, request);
        return ApiResult.success("更新成功", response);
    }

    // ==================== 17.5 删除表单模板 ====================

    /**
     * 删除表单模板（DELETE /admin/form-templates/{templateId}，文档 17.5）
     * <p>
     * 软删除；当前为默认模板（is_default=1）时不允许删除，需先取消默认。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 删除结果
     */
    @AuditLog(module = "form-template", action = "delete",
            description = "删除表单模板: #templateId", logResult = true)
    @DeleteMapping("/{templateId}")
    public ApiResult<FormTemplateDeleteResponse> deleteTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        FormTemplateDeleteResponse response = adminFormTemplateService.deleteTemplate(userId, templateId);
        return ApiResult.success("删除成功", response);
    }

    // ==================== 17.6 设置默认表单模板 ====================

    /**
     * 设置默认表单模板（PUT /admin/form-templates/{templateId}/default，文档 17.6）
     * <p>
     * 将指定模板设为某学校、某 code + category 下的默认模板，同时取消同校同 code + category 下
     * 其他模板的默认状态。被设置模板必须处于启用状态。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 设置结果
     */
    @AuditLog(module = "form-template", action = "set-default",
            description = "设置默认表单模板: #templateId", logResult = true)
    @PutMapping("/{templateId}/default")
    public ApiResult<FormTemplateDefaultResponse> setDefaultTemplate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long templateId) {
        FormTemplateDefaultResponse response = adminFormTemplateService.setDefaultTemplate(userId, templateId);
        return ApiResult.success("设置成功", response);
    }
}
