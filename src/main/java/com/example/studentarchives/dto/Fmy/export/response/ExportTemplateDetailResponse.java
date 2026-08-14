package com.example.studentarchives.dto.Fmy.export.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出模板详情响应 DTO（GET /admin/export-templates/{templateId}，管理端文档 5.4）
 * <p>
 * 返回模板完整配置：字段配置、模板内容、页眉页脚、水印与字体配置等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportTemplateDetailResponse {

    private Long id;

    private Long schoolId;

    private String templateName;

    private String templateCode;

    private String exportType;

    /** 导出类型中文标签 */
    private String exportTypeLabel;

    private Integer scopeType;

    /** 范围类型中文标签 */
    private String scopeTypeLabel;

    /** 字段配置（JSON，按列配置渲染） */
    private JsonNode fieldsConfig;

    /** 默认筛选条件（JSON） */
    private JsonNode filterConditions;

    private Integer templateMode;

    /** 模板渲染模式中文标签 */
    private String templateModeLabel;

    /** HTML 模板内容（自由模板模式） */
    private String templateContent;

    private String engineType;

    /** 页面配置（JSON） */
    private JsonNode pageConfig;

    private String paperSize;

    /** 1=纵向 2=横向 */
    private Integer orientation;

    /** 方向中文标签 */
    private String orientationLabel;

    /** 页边距配置（JSON） */
    private JsonNode marginConfig;

    private String headerHtml;

    private String footerHtml;

    /** 水印配置（JSON） */
    private JsonNode watermarkConfig;

    /** 字体配置（JSON） */
    private JsonNode fontConfig;

    private String previewImage;

    private Integer version;

    private Integer isDefault;

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
