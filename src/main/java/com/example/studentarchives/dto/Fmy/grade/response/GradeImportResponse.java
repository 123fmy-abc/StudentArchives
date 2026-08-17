package com.example.studentarchives.dto.Fmy.grade.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入成绩响应 DTO（POST /admin/grades/import，文档 13.1）
 * <p>
 * 导入任务异步执行，接口立即返回任务 ID 与初始状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeImportResponse {

    /** 导入任务 ID（grade_import_logs.id） */
    private Long importId;

    /** 导入状态：0=导入中 1=完成 2=失败 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 预估耗时（秒） */
    private Integer estimatedSeconds;
}
