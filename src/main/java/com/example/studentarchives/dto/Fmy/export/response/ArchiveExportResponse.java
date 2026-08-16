package com.example.studentarchives.dto.Fmy.export.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 一键导出学生档案（管理端）提交响应
 * <p>对应管理端接口文档 5.11 POST /admin/exports/archives 响应体</p>
 */
@Getter
@Builder
public class ArchiveExportResponse {

    /** 导出任务ID，用于轮询 5.2 查询导出任务状态 */
    private Long jobId;

    /** 导出类型 */
    private String exportType;

    /** 任务状态：0待处理 1执行中 2完成 3失败 */
    private Integer status;

    /** 任务状态说明 */
    private String statusLabel;

    /** 预计耗时（秒） */
    private Long estimatedSeconds;
}
