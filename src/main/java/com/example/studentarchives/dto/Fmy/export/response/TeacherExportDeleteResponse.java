package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师端删除导出任务响应 DTO（DELETE /teacher/exports/{jobId}，教师端文档 12.4）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExportDeleteResponse {

    /** 已删除的导出任务 ID（export_jobs.id） */
    private Long jobId;

    /** 删除时间（ISO 8601 带时区） */
    private String deletedAt;
}
