package com.example.studentarchives.dto.Fmy.formtemplate.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单模板渲染响应 DTO（GET /common/form-templates，《学生端/教师端接口文档》通用接口）
 * <p>
 * 面向学生端/教师端表单渲染，仅返回渲染所需字段，不含管理端审计字段（createdBy 等），
 * 避免复用 {@code FormTemplateDetailResponse} 泄漏管理端信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateRenderResponse {

    private Long id;

    private String code;

    private String category;

    private String templateName;

    private Integer version;

    /** 字段配置数组（JSON 列解析） */
    private JsonNode fields;

    /** 布局配置 JSON（JSON 列解析） */
    private JsonNode layoutConfig;

    /** 适用角色编码数组（JSON 列解析），null 表示全部角色 */
    private List<String> applicableRoles;
}
