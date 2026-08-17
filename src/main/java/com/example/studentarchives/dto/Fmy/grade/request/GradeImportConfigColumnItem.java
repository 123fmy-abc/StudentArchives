package com.example.studentarchives.dto.Fmy.grade.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 成绩导入配置模板列项（POST/PUT /admin/grade-import-configs）
 * <p>
 * 对应 {@code grade_import_configs.template_columns} JSON 数组中的单个列定义。
 */
@Data
public class GradeImportConfigColumnItem {

    /** 字段名，解析后映射到 gpa_records 等落库字段 */
    @NotBlank(message = "field 不能为空")
    private String field;

    /** 模板表头显示名称 */
    @NotBlank(message = "label 不能为空")
    private String label;

    /** 是否必填列 */
    private Boolean required = false;
}
