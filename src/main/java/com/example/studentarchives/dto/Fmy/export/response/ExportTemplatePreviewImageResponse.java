package com.example.studentarchives.dto.Fmy.export.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传导出模板预览图响应 DTO（POST /admin/export-templates/{templateId}/preview-image，管理端文档 5.10）
 * <p>
 * 上传成功后返回模板 ID、预览图完整访问 URL（带签名，30 天有效）及 OSS 对象路径。
 * 再次调用同一接口即覆盖原预览图（upsert），不触发 version 自增。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportTemplatePreviewImageResponse {

    /** 模板 ID（export_templates.id） */
    private Long id;

    /** 预览图完整访问 URL（带签名，30 天有效） */
    private String previewImage;

    /** OSS 对象路径（export-template-preview/{uuid}.{ext}） */
    private String objectKey;

    /** 更新时间（ISO 8601 带时区） */
    private String updatedAt;
}
