package com.example.studentarchives.dto.Fmy.export.request;

import lombok.Data;

/**
 * 修改导出模板状态请求 DTO（PATCH /admin/export-templates/{templateId}/status，管理端文档 5.9）
 * <p>
 * 状态变更为操作状态变更，不触发 version 自增（version 记录模板内容版本，供导出任务追溯），
 * 故与通用更新接口（5.6）拆分，避免状态开关污染内容版本号。
 */
@Data
public class ExportTemplateStatusRequest {

    /** 0=禁用 1=启用 */
    private Integer status;
}
