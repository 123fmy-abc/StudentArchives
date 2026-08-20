package com.example.studentarchives.dto.Fmy.formtemplate.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新表单模板响应 DTO（PUT /admin/form-templates/{templateId}，《管理端接口文档》17.4）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateUpdateResponse {

    /** 模板 ID（form_templates.id） */
    private Long id;

    private Long schoolId;

    private String templateName;

    private String code;

    private String category;

    /** 更新后版本号（每次更新自动 +1） */
    private Integer version;

    private Integer isDefault;

    private Integer status;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
