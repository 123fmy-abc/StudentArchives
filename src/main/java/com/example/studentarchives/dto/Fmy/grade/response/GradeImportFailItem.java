package com.example.studentarchives.dto.Fmy.grade.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入失败明细项 DTO（GET /admin/grades/imports/{importId}，文档 13.3）
 * <p>
 * 对应 grade_import_logs.fail_details 中的单条失败记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportFailItem {

    /** 数据行号（不含表头，从 1 开始） */
    private Integer row;

    /** 学生学号 */
    private String studentNo;

    /** 失败原因，如「学号不存在」「成绩格式错误」 */
    private String reason;
}
