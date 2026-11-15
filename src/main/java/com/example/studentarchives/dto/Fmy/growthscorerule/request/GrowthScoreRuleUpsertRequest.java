package com.example.studentarchives.dto.Fmy.growthscorerule.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成长事件计分规则创建/更新请求（POST、PUT /admin/growth-source-score-rules）
 * <p>
 * 每种来源记录类型（source_type）仅允许一条规则；abilities 内 dimensionCode
 * 不得重复，且必须存在于 ability_dimensions 中并处于启用状态（Service 层校验）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthScoreRuleUpsertRequest {

    /** 来源记录类型：archives / award_applications / archive_internships 等 */
    @NotBlank(message = "来源记录类型不能为空")
    @Size(max = 64, message = "来源记录类型长度不能超过64")
    private String sourceType;

    /** 该类来源记录的维度得分项（至少一项） */
    @NotEmpty(message = "能力维度得分项不能为空")
    @Valid
    private List<GrowthScoreRuleItem> abilities;

    /** 备注（可空） */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
