package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 画像维度平均分 DTO（统计看板/组织下钻共用）
 * <p>
 * 各能力维度平均得分，用于雷达图/维度对比，来自快照 top_dimensions 或
 * portrait_evaluation_scores 聚合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionAvgScoreItem {

    /** 维度编码，如 academic / competition / practice */
    private String dimensionCode;

    /** 维度名称，如 学业成绩 */
    private String dimensionName;

    /** 平均分（0-100） */
    private Double avgScore;
}
