package com.example.studentarchives.dto.Fmy.formtemplate.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单模板详情响应 DTO（GET /admin/form-templates/{templateId}，《管理端接口文档》17.2）
 * <p>
 * 返回模板完整配置：字段配置 fields、布局配置 layoutConfig（JSON 列解析为 JsonNode）、
 * 适用角色与版本信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateDetailResponse {

    private Long id;

    private Long schoolId;

    private String templateName;

    private String code;

    private String category;

    /** 适用类别中文标签 */
    private String categoryLabel;

    private String description;

    /** 字段配置数组（JSON 列解析） */
    private JsonNode fields;

    /** 布局配置 JSON（JSON 列解析） */
    private JsonNode layoutConfig;

    /** 适用角色编码数组（JSON 列解析），null 表示全部角色 */
    private List<String> applicableRoles;

    /** 0=非默认 1=默认模板 */
    private Integer isDefault;

    private Integer version;

    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    private Long createdBy;

    /** 创建人姓名 */
    private String createdByName;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
