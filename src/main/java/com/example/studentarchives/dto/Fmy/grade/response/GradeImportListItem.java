package com.example.studentarchives.dto.Fmy.grade.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入历史列表项 DTO（GET /admin/grades/imports，文档 13.2）
 * <p>
 * 对应 grade_import_logs 表，按 created_at 倒序。semesterName 由 semester_id 关联
 * semesters.name 得到，operatorName 由 operator_id 关联 users.name 得到。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportListItem {

    /** 导入任务 ID */
    private Long id;

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    /** 操作人姓名 */
    private String operatorName;

    /** 成绩文件原始文件名 */
    private String fileName;

    /** 总记录数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 导入状态：0=导入中 1=完成 2=失败 */
    private Integer importStatus;

    /** 状态中文标签 */
    private String importStatusLabel;

    /** 开始时间（ISO 8601 带时区） */
    private String startedAt;

    /** 完成时间（ISO 8601 带时区） */
    private String completedAt;
}
