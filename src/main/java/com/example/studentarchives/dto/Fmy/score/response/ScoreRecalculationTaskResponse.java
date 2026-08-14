package com.example.studentarchives.dto.Fmy.score.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评分重算任务进度响应 DTO（GET /admin/scores/recalculation-tasks/{taskId}，文档 2.2）
 * <p>
 * 数据来源：score_recalculation_tasks 表。
 * status 枚举：0=排队中 1=执行中 2=完成 3=失败。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRecalculationTaskResponse {

    /** 任务 ID */
    private Long id;

    /** 重算范围（1=指定学生 2=指定班级 3=指定学期 4=全量重算 5=指定专业） */
    private Integer targetType;

    /** 对应范围 ID */
    private Long targetId;

    /** 学期 ID */
    private Long semesterId;

    /** 任务状态：0=排队中 1=执行中 2=完成 3=失败 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 待计算记录总数 */
    private Integer totalCount;

    /** 成功计算数 */
    private Integer successCount;

    /** 失败计算数 */
    private Integer failCount;

    /** 开始执行时间（ISO 8601 带时区） */
    private String startedAt;

    /** 完成时间（ISO 8601 带时区） */
    private String completedAt;

    /** 失败原因 */
    private String errorMessage;

    /** 任务状态提示消息（排队中/执行中/完成摘要/失败原因等） */
    private String message;

    /** 失败明细列表（status=2 且 failCount>0 时由 errorMessage 中的 JSON 解析而来；其余情况为 null） */
    private List<StudentFailureItem> failures;

    /** 单个学生重算失败明细 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentFailureItem {

        /** 学生用户 ID */
        private Long userId;

        /** 失败原因 */
        private String message;
    }
}
