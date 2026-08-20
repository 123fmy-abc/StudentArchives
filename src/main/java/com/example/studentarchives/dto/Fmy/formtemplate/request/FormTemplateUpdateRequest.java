package com.example.studentarchives.dto.Fmy.formtemplate.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新表单模板请求 DTO（PUT /admin/form-templates/{templateId}，《管理端接口文档》17.4）
 * <p>
 * 全部字段可选，未传（null）表示不修改；更新成功时 version 自动 +1。
 * code / category / schoolId 不支持修改（文档 17.4 请求参数不含三者）。
 * status 支持在本接口直接变更（与导出模板不同，文档 17.4 明确包含 status）。
 */
@Data
public class FormTemplateUpdateRequest {

    @Size(max = 100, message = "模板名称最大 100 字符")
    private String templateName;

    @Size(max = 255, message = "模板说明最大 255 字符")
    private String description;

    /** 字段配置数组（全量覆盖） */
    @Valid
    private List<FormTemplateFieldItem> fields;

    /** 布局配置 JSON */
    private Object layoutConfig;

    /** 适用角色编码数组；不传表示不修改 */
    private List<String> applicableRoles;

    /** 0=禁用 1=启用 */
    private Integer status;
}
