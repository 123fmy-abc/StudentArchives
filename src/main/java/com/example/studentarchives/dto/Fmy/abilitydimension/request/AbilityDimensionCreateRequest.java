package com.example.studentarchives.dto.Fmy.abilitydimension.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建能力维度请求 DTO（POST /admin/ability-dimensions）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityDimensionCreateRequest {

    /** 维度名称 */
    @NotBlank(message = "dimensionName 不能为空")
    @Size(max = 50, message = "dimensionName 长度不能超过50")
    private String dimensionName;

    /** 维度编码（全局唯一，关联 evaluation_indicators.dimension_code） */
    @NotBlank(message = "dimensionCode 不能为空")
    @Size(max = 50, message = "dimensionCode 长度不能超过50")
    private String dimensionCode;

    /** 维度说明 */
    @Size(max = 255, message = "description 长度不能超过255")
    private String description;

    /** 排序号，越小越靠前 */
    @NotNull(message = "sort 不能为空")
    private Integer sort;
}
