package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 画像分数列表响应 DTO（GET /profile/scores）
 * <p>
 * 数据来源：portrait_evaluation_scores、ability_dimensions、indicator_rule_versions、semesters。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScoreListResponse {

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称，如 "2023-2024-1" */
    private String semesterName;

    /** 评分时间（ISO 8601 带时区），对应 portrait_evaluation_scores.evaluated_at */
    private String calculatedAt;

    /** 指标规则版本（indicator_rule_versions.version） */
    private Integer ruleVersion;

    /** 评分计算批次 ID */
    private Long calculationId;

    /** 各维度分数列表 */
    private List<ScoreItem> list;

    /**
     * 维度分数项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreItem {

        private String dimensionCode;

        private String dimensionName;

        /** 当前得分 */
        private BigDecimal score;

        /** 目标分 */
        private BigDecimal targetScore;

        /** 与目标差距 */
        private BigDecimal gap;

        /** 较上一阶段变化，如 "+7" / "-3.5" */
        private String change;

        /** 对比学期 ID */
        private Long comparedSemesterId;

        /** 对比学期名称 */
        private String comparedSemesterName;

        /** 单位，如 "分" */
        private String unit;
    }
}
