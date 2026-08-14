package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除导出模板响应 DTO（DELETE /admin/export-templates/{templateId}，管理端文档 5.7）
 * <p>
 * 采用逻辑删除：记录 deleted_at 置位，历史导出任务不受影响。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateDeleteResponse {

    /** 模板 ID（export_templates.id） */
    private Long id;

    /** 逻辑删除时间（ISO 8601 带时区） */
    private String deletedAt;
}
