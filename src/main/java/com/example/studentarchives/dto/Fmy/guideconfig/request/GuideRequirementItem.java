package com.example.studentarchives.dto.Fmy.guideconfig.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评选必填字段要求项（评选说明配置的 requirements 元素）
 * <p>
 * {@code field} 必须为对应业务提交 DTO 的属性名，否则前端渲染校验会失效。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuideRequirementItem {

    /** 业务字段名（如 competitionName） */
    @NotBlank(message = "评选要求字段名不能为空")
    @Size(max = 64, message = "评选要求字段名长度不能超过64")
    private String field;

    /** 字段中文名（如 竞赛名称） */
    @NotBlank(message = "评选要求字段中文名不能为空")
    @Size(max = 100, message = "评选要求字段中文名长度不能超过100")
    private String label;

    /** 是否必填 */
    private boolean required;

    /** 填写说明（可空） */
    @Size(max = 255, message = "填写说明长度不能超过255")
    private String description;
}
