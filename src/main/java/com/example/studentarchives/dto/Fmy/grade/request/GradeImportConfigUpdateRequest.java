package com.example.studentarchives.dto.Fmy.grade.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 成绩导入配置更新请求 DTO（PUT /admin/grade-import-configs/{id}）
 * <p>
 * 全部字段可选，未传（null）表示不修改；传入空集合会覆盖原值。
 */
@Data
public class GradeImportConfigUpdateRequest {

    /** 允许上传的文件扩展名列表 */
    private List<String> allowedExtensions;

    /** 单个文件最大字节数 */
    private Long maxFileSize;

    /** 模板列定义 */
    @Valid
    private List<GradeImportConfigColumnItem> templateColumns;

    /** 是否首行为表头：0=否 1=是 */
    private Integer hasHeaderRow;

    /** 单批次处理行数 */
    private Integer batchSize;

    /** 导入模式：0=追加 1=允许覆盖 */
    private Integer allowOverwrite;

    /**
     * 状态：0=禁用 1=启用
     * <p>
     * 已废弃：状态变更已拆分为独立接口（PATCH /admin/grade-import-configs/{id}/status），
     * 本接口传 status 返回 10001 参数错误。
     */
    private Integer status;
}
