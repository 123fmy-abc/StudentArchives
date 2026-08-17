package com.example.studentarchives.dto.Fmy.grade.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 成绩导入配置保存请求 DTO（POST /admin/grade-import-configs）
 * <p>
 * 每个学校仅允许一条启用配置，创建时若已存在则返回参数错误。
 */
@Data
public class GradeImportConfigSaveRequest {

    /** 允许上传的文件扩展名列表，如 ["xlsx","csv"] */
    @NotEmpty(message = "allowedExtensions 不能为空")
    private List<String> allowedExtensions;

    /** 单个文件最大字节数，0 表示无限制 */
    @NotNull(message = "maxFileSize 不能为空")
    private Long maxFileSize;

    /** 模板列定义 */
    @NotEmpty(message = "templateColumns 不能为空")
    @Valid
    private List<GradeImportConfigColumnItem> templateColumns;

    /** 是否首行为表头：0=否 1=是，默认 1 */
    private Integer hasHeaderRow = 1;

    /** 单批次处理行数，默认 500 */
    private Integer batchSize = 500;

    /** 导入模式：0=追加 1=允许覆盖，默认 0 */
    private Integer allowOverwrite = 0;

    /** 状态：0=禁用 1=启用，默认 1 */
    private Integer status = 1;
}
