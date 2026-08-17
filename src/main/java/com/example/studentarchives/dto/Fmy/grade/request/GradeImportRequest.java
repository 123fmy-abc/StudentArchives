package com.example.studentarchives.dto.Fmy.grade.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入成绩请求 DTO（POST /admin/grades/import，文档 13.1）
 * <p>
 * 上传成绩文件并启动异步导入任务，文件支持 .xlsx / .csv（.xls 不解析）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportRequest {

    /** 学期 ID（对应 grade_import_logs.semester_id） */
    @NotNull(message = "semesterId 不能为空")
    private Long semesterId;

    /** 上传的成绩文件 ID（先调用文件上传接口获取 file_uploads.id） */
    @NotNull(message = "fileId 不能为空")
    private Long fileId;

    /** 是否覆盖已存在的成绩记录，默认 false（追加模式） */
    private Boolean overwrite;
}
