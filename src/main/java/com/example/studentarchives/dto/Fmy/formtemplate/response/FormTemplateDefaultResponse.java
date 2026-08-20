package com.example.studentarchives.dto.Fmy.formtemplate.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置默认表单模板响应 DTO（PUT /admin/form-templates/{templateId}/default，《管理端接口文档》17.6）
 * <p>
 * 默认模板：同一学校、同一 code + category 下 is_default = 1 的唯一模板。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateDefaultResponse {

    /** 被设置为默认的模板 ID（form_templates.id） */
    private Long id;

    private Long schoolId;

    private String code;

    private String category;

    /** 模板名称 */
    private String templateName;

    /** 是否已设为默认（恒为 1） */
    private Integer isDefault;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
