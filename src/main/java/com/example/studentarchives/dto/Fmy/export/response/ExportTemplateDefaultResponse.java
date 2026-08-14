package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置默认导出模板响应 DTO（PUT /admin/export-templates/{templateId}/default，管理端文档 5.8）
 * <p>
 * 默认模板：同一学校同一导出类型下 is_default = 1 的唯一模板。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateDefaultResponse {

    /** 被设置为默认的模板 ID（export_templates.id） */
    private Long id;

    private Long schoolId;

    private String exportType;

    /** 模板名称 */
    private String templateName;

    /** 是否已设为默认（恒为 1） */
    private Integer isDefault;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
