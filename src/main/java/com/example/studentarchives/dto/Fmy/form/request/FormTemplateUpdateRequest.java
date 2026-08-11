package com.example.studentarchives.dto.Fmy.form.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表单模板更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateUpdateRequest {

    @Size(max = 100, message = "templateName 长度不能超过100")
    private String templateName;

    @Size(max = 255, message = "description 长度不能超过255")
    private String description;

    /** 字段定义 JSON 数组 */
    private JsonNode fields;

    /** 布局配置 JSON */
    private JsonNode layoutConfig;

    /** 适用角色编码 JSON 数组 */
    private JsonNode applicableRoles;

    /** 0=禁用 1=启用 */
    private Integer status;
}
