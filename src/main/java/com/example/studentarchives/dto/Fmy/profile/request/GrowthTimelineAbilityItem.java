package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 成长时间轴能力维度得分输入项（POST /profile/growth-timeline、PUT /profile/growth-timeline/{id}）
 * <p>
 * 对应表 growth_timeline_abilities。维度编码为展示型引用（非外键），
 * 与 ability_dimensions.dimension_code 对齐，未知编码时详情响应的 dimensionName 为 null。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthTimelineAbilityItem {

    /** 能力维度编码（对应 ability_dimensions.dimension_code） */
    @NotBlank(message = "能力维度编码不能为空")
    @Size(max = 50, message = "能力维度编码长度不能超过50")
    private String dimensionCode;

    /** 该事件带来的维度得分变化（0-100） */
    @DecimalMin(value = "0", message = "能力得分不能小于0")
    @DecimalMax(value = "100", message = "能力得分不能大于100")
    private BigDecimal score;
}
