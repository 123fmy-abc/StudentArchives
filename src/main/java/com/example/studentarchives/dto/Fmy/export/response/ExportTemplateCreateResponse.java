package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建导出模板响应 DTO（POST /admin/export-templates，管理端文档 5.5）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateCreateResponse {

    /** 模板 ID（export_templates.id） */
    private Long id;

    private Long schoolId;

    private String templateName;

    /** 模板编码 */
    private String templateCode;

    private String exportType;

    private Integer templateMode;

    /** 初始版本号 */
    private Integer version;

    private Integer isDefault;

    private Integer status;

    private Long createdBy;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
