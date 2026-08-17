package com.example.studentarchives.dto.Fmy.grade.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成绩导入配置状态变更请求 DTO（PATCH /admin/grade-import-configs/{id}/status）
 */
@Data
public class GradeImportConfigStatusRequest {

    /** 0=禁用 1=启用 */
    @NotNull(message = "status 不能为空")
    private Integer status;
}
