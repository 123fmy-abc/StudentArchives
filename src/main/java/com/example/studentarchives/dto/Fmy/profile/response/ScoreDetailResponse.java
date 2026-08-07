package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分数计算说明响应 DTO（GET /profile/scores/{calculationId}/details）
 * <p>
 * 数据来源：score_calculations、score_calculation_details、evaluation_indicators、
 * indicator_rule_versions、archives。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScoreDetailResponse {

    /** 计算批次 ID */
    private Long calculationId;

    /** 计算时间（ISO 8601 带时区），对应 score_calculations.calculated_at */
    private String calculatedAt;

    /** 指标规则版本 */
    private Integer ruleVersion;

    /** 数据来源说明 */
    private String dataSource;

    /** 指标明细列表 */
    private List<ScoreDetailItem> details;

    /**
     * 指标明细项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDetailItem {

        /** 指标 ID（evaluation_indicators.id） */
        private Long indicatorId;

        /** 指标名称 */
        private String indicatorName;

        private String dimensionCode;

        private String dimensionName;

        /** 指标权重（score_calculation_details.weight） */
        private BigDecimal weight;

        /** 原始得分 */
        private BigDecimal rawScore;

        /** 加权得分 */
        private BigDecimal weightedScore;

        /** 来源档案 ID 列表 */
        private List<Long> sourceArchiveIds;

        /** 来源档案标题列表 */
        private List<String> sourceArchiveTitles;
    }
}
