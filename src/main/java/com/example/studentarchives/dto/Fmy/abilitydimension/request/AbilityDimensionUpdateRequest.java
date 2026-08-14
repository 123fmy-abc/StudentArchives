package com.example.studentarchives.dto.Fmy.abilitydimension.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新能力维度请求 DTO（PUT /admin/ability-dimensions/{id}）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityDimensionUpdateRequest {

    /** 维度名称 */
    @Size(max = 50, message = "dimensionName 长度不能超过50")
    private String dimensionName;

    /** 维度编码（全局唯一） */
    @Size(max = 50, message = "dimensionCode 长度不能超过50")
    private String dimensionCode;

    /** 维度说明 */
    @Size(max = 255, message = "description 长度不能超过255")
    private String description;

    /** 排序号 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;
}
