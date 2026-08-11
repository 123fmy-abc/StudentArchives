package com.example.studentarchives.dto.Fmy.indicator.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端指标树响应 DTO（GET /admin/indicators/tree，文档 1.1）
 * <p>
 * 返回完整的指标树，包含每个指标的状态、计分规则与当前版本信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminIndicatorTreeResponse {

    /** 指标规则版本 ID（对应 indicator_rule_versions.id），未发布过则为 null */
    private Long versionId;

    /** 全局规则版本号（对应 indicator_rule_versions.version） */
    private Integer version;

    /** 版本名称 */
    private String versionName;

    /** 生效时间（ISO 8601 带时区） */
    private String effectiveAt;

    /** 一级指标节点列表 */
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
        private Long id;

        /** 指标编码 */
        private String indicatorCode;

        /** 指标名称 */
        private String indicatorName;

        /** 层级：1/2/3 */
        private Integer level;

        /** 权重（0-1） */
        private BigDecimal weight;

        /** 0=禁用 1=启用 */
        private Integer status;

        /** 状态标签：启用/禁用 */
        private String statusLabel;

        /** 当前版本号（对应 evaluation_indicators.version） */
        private Integer version;

        /** 能力维度编码 */
        private String dimensionCode;

        /** 能力维度名称 */
        private String dimensionName;

        /** 指标说明 */
        private String description;

        /** 计分规则 JSON 对象（三级指标） */
        private JsonNode scoringRule;

        /** 排序 */
        private Integer sort;

        /** 子指标节点列表 */
        private List<IndicatorNode> children;
    }
}
