package com.example.studentarchives.dto.Fmy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 指标树查询响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndicatorTreeResponse {

    /** 指标版本 ID（对应 indicator_rule_versions.id） */
    private Long versionId;

    /** 版本名称（对应 indicator_rule_versions.version_name） */
    private String versionName;

    /** 生效时间（对应 indicator_rule_versions.effective_at） */
    private String effectiveAt;

    /** 指标树节点列表（一级节点） */
    private List<IndicatorNode> indicators;

    /**
     * 指标树节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IndicatorNode {

        /** 指标 ID（对应 evaluation_indicators.id） */
        private Long indicatorId;

        /** 指标编码 */
        private String indicatorCode;

        /** 指标名称 */
        private String indicatorName;

        /** 层级：1/2/3 */
        private Integer level;

        /** 权重 */
        private BigDecimal weight;

        /** 维度编码 */
        private String dimensionCode;

        /** 维度名称 */
        private String dimensionName;

        /** 子节点列表 */
        private List<IndicatorNode> children;
    }
}
