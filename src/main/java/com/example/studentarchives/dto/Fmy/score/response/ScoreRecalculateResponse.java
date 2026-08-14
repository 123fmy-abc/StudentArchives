package com.example.studentarchives.dto.Fmy.score.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 触发评分重算响应 DTO（POST /admin/scores/recalculate，文档 2.1）
 * <p>
 * 任务进入 score_recalculation_tasks 异步执行，接口立即返回任务 ID。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRecalculateResponse {

    /** 评分重算任务 ID（score_recalculation_tasks.id） */
    private Long taskId;

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

    /** 触发时间（ISO 8601 带时区） */
    private String createdAt;

    /** 业务提示消息（如「评分重算任务已创建」） */
    private String message;
}
