package com.example.studentarchives.dto.Fmy.growthscorerule.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成长事件计分规则能力维度得分项
 * <p>
 * 与 growth_event_score_rules.abilities JSON 元素结构一致：
 * dimensionCode 对应 ability_dimensions.dimension_code，score 为该类事件对该维度的得分贡献（0-100）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthScoreRuleItem {

    /** 能力维度编码（对应 ability_dimensions.dimension_code，须存在且启用） */
    @NotBlank(message = "能力维度编码不能为空")
    @Size(max = 50, message = "能力维度编码长度不能超过50")
    private String dimensionCode;

    /** 该类事件带来的维度得分（0-100） */
    @NotNull(message = "能力得分不能为空")
    @DecimalMin(value = "0", message = "能力得分不能小于0")
    @DecimalMax(value = "100", message = "能力得分不能大于100")
    private BigDecimal score;
}
