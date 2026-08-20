package com.example.studentarchives.dto.Fmy.formtemplate.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建表单模板响应 DTO（POST /admin/form-templates，《管理端接口文档》17.3）
 * <p>
 * 创建成功后 version 初始为 1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateCreateResponse {

    /** 模板 ID（form_templates.id） */
    private Long id;

    private Long schoolId;

    private String templateName;

    private String code;

    private String category;

    /** 初始版本号（恒为 1） */
    private Integer version;

    private Integer isDefault;

    private Integer status;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
