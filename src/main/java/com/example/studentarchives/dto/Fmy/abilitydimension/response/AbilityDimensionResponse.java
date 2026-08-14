package com.example.studentarchives.dto.Fmy.abilitydimension.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 能力维度响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbilityDimensionResponse {

    /** 维度 ID */
    private Long id;

    /** 维度名称 */
    private String dimensionName;

    /** 维度编码 */
    private String dimensionCode;

    /** 维度说明 */
    private String description;

    /** 排序号 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;

    /** 状态标签 */
    private String statusLabel;
}
