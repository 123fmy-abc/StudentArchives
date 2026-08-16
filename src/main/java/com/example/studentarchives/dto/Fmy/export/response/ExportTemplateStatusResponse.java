package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改导出模板状态响应 DTO（PATCH /admin/export-templates/{templateId}/status，管理端文档 5.9）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateStatusResponse {

    /** 模板 ID（export_templates.id） */
    private Long id;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 状态中文标签（禁用/启用） */
    private String statusLabel;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
