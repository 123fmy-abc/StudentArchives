package com.example.studentarchives.dto.Fmy.grade.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入详情响应 DTO（GET /admin/grades/imports/{importId}，文档 13.3）
 * <p>
 * 在列表项基础上增加 operatorId、fileId 与失败明细列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportDetailResponse {

    /** 导入任务 ID */
    private Long id;

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 成绩文件 ID（file_uploads.id） */
    private Long fileId;

    /** 总记录数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 失败明细（行号/学号/失败原因） */
    private List<GradeImportFailItem> failDetails;

    /** 导入状态：0=导入中 1=完成 2=失败 */
    private Integer importStatus;

    /** 状态中文标签 */
    private String importStatusLabel;

    /** 开始时间（ISO 8601 带时区） */
    private String startedAt;

    /** 完成时间（ISO 8601 带时区） */
    private String completedAt;
}
