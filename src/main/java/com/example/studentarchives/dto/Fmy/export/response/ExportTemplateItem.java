package com.example.studentarchives.dto.Fmy.export.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出模板列表项 DTO（GET /admin/export-templates，管理端文档 5.3）
 * <p>
 * 列表页不返回 templateContent / headerHtml / footerHtml 等大字段，需调详情接口获取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportTemplateItem {

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

    /** 模板渲染模式：1=字段列表模式 2=自由模板模式 */
    private Integer templateMode;

    private String engineType;

    private String paperSize;

    /** 1=纵向 2=横向 */
    private Integer orientation;

    /** 方向中文标签 */
    private String orientationLabel;

    private Integer version;

    private Integer isDefault;

    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    private String previewImage;

    private Long createdBy;

    /** 创建人姓名 */
    private String createdByName;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
