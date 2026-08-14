package com.example.studentarchives.dto.Fmy.score.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 触发评分重算请求 DTO（POST /admin/scores/recalculate，文档 2.1）
 * <p>
 * targetType 映射 score_recalculation_tasks.task_type：
 * 1=指定学生 2=指定班级 3=指定学期 4=全量重算 5=指定专业。
 * targetType=4(全量) 时无需传 targetId；targetType=1/2/5 时 targetId 必填。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRecalculateRequest {

    /** 重算范围：1=指定学生 2=指定班级 3=指定学期 4=全量重算 5=指定专业 */
    @NotNull(message = "targetType 不能为空")
    @Min(value = 1, message = "targetType 取值范围 1-5")
    @Max(value = 5, message = "targetType 取值范围 1-5")
    private Integer targetType;

    /** 对应范围 ID：指定学生=学生ID，指定班级=班级ID，指定专业=专业ID；targetType=4 时无需传 */
    private Long targetId;

    /** 学期 ID */
    @NotNull(message = "semesterId 不能为空")
    private Long semesterId;
}
