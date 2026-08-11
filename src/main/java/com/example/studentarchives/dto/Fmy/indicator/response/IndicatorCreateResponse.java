package com.example.studentarchives.dto.Fmy.indicator.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建指标响应 DTO（POST /admin/indicators，文档 1.2）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCreateResponse {

    /** 指标 ID */
    private Long id;

    /** 指标编码 */
    private String indicatorCode;

    /** 指标名称 */
    private String indicatorName;

    /** 层级：1/2/3 */
    private Integer level;

    /** 权重 */
    private BigDecimal weight;

    /** 0=禁用 1=启用 */
    private Integer status;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
