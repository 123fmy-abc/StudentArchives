package com.example.studentarchives.dto.Fmy.growthscorerule.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成长事件计分规则创建/更新请求（POST、PUT /admin/growth-event-score-rules）
 * <p>
 * 每种事件类型（1-6）仅允许一条规则；abilities 内 dimensionCode 不得重复，
 * 且必须存在于 ability_dimensions 中并处于启用状态（Service 层校验）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthScoreRuleUpsertRequest {

    /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
    @NotNull(message = "事件类型不能为空")
    @Min(value = 1, message = "事件类型只能是1-6")
    @Max(value = 6, message = "事件类型只能是1-6")
    private Integer eventType;

    /** 该类事件的能力维度得分项（至少一项） */
    @NotEmpty(message = "能力维度得分项不能为空")
    @Valid
    private List<GrowthScoreRuleItem> abilities;

    /** 备注（可空） */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
