package com.example.studentarchives.dto.Fmy.export.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师端可导出模板列表项 DTO（GET /teacher/exports/templates，教师端文档 12.1）
 * <p>
 * 复用 {@code export_templates} 表数据，仅返回教师导出可用的精简字段；
 * {@code templateId} 与 12.2 提交导出任务的请求字段 {@code templateId} 对称（对应 {@code export_templates.id}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherExportTemplateResponse {

    /** 模板 ID（export_templates.id） */
    private Long templateId;

    /** 模板名称 */
    private String templateName;

    /** 导出类型：student_archive 等 */
    private String exportType;

    /** 导出类型中文标签 */
    private String exportTypeLabel;

    /** 范围类型：1学校 2学院 3专业 4班级 6年级 */
    private Integer scopeType;

    /** 范围类型中文标签 */
    private String scopeTypeLabel;
}
