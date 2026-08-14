package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新导出模板响应 DTO（PUT /admin/export-templates/{templateId}，管理端文档 5.6）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateUpdateResponse {

    /** 模板 ID（export_templates.id） */
    private Long id;

    private Long schoolId;

    private String templateName;

    private String templateCode;

    private String exportType;

    private Integer templateMode;

    /** 更新后版本号（每次更新自动 +1） */
    private Integer version;

    private Integer isDefault;

    private Integer status;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
