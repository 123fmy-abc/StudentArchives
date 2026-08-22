package com.example.studentarchives.dto.Fmy.export.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师端导出任务列表项 DTO（GET /teacher/exports，教师端文档 12.3）
 * <p>
 * 数据来源 {@code export_jobs}（按 {@code operator_id} 过滤当前教师），
 * {@code templateName} 由 {@code template_id} → {@code export_templates} 解析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherExportJobItem {

    /** 导出任务 ID（export_jobs.id） */
    private Long exportJobId;

    /** 模板名称（export_templates.template_name，模板已删除时为 null） */
    private String templateName;

    /** 导出类型：student_archive 等 */
    private String exportType;

    /** 任务状态：0=待执行 1=执行中 2=完成 3=失败 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 导出总记录数 */
    private Integer totalCount;

    /** 导出成功记录数 */
    private Integer successCount;

    /** 下载链接（任务完成且有生成文件时返回，指向 GET /common/files/{fileId}/download） */
    private String downloadUrl;

    /** 链接过期时间（ISO 8601 带时区），未完成时为 null */
    private String expireAt;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
