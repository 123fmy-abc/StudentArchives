package com.example.studentarchives.dto.Fmy.export.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 研究数据导出响应 DTO（POST /admin/exports/research，管理端文档 5.1）
 * <p>
 * 任务进入 export_jobs 异步执行，接口立即返回任务 ID 与初始状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchExportResponse {

    /** 导出任务 ID（export_jobs.id） */
    private Long jobId;

    /** 任务状态：0=待执行 1=执行中 2=完成 3=失败 */
    private Integer status;

    /** 预计耗时（秒），供前端展示 */
    private Integer estimatedSeconds;
}
