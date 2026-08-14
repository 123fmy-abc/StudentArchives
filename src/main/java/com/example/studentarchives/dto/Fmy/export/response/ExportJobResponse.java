package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出任务进度响应 DTO（GET /admin/exports/{jobId}，管理端文档 5.2）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponse {

    /** 任务 ID（export_jobs.id） */
    private Long id;

    /** 导出类型：archive_research 等 */
    private String exportType;

    /** 任务状态：0=待执行 1=执行中 2=完成 3=失败 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 下载链接（任务完成且有生成文件时返回） */
    private String downloadUrl;

    /** 链接过期时间（ISO 8601 带时区），未完成时为 null */
    private String expireAt;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;

    /** 完成时间（ISO 8601 带时区），未完成时为 null */
    private String completedAt;
}
