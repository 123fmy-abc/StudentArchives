package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 组织下钻多维汇总行 DTO（GET /admin/statistics/overview，文档 16.2）
 * <p>
 * 单个下级组织的多维汇总（成绩、奖项、实践、爱好、维度得分、类型分布）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgOverviewRow {

    /** 组织 ID */
    private Long orgId;

    /** 组织名称 */
    private String orgName;

    /** 学生数 */
    private Integer studentCount;

    /** 档案总数 */
    private Integer archiveCount;

    /** 获奖总数 */
    private Integer awardCount;

    /** 平均绩点 */
    private Double avgGpa;

    /** 平均分 */
    private Double avgScore;

    /** 实践档案数 */
    private Integer practiceCount;

    /** 热门兴趣 TopN 标签 */
    private List<String> topInterests;

    /** 画像维度平均分 */
    private List<DimensionAvgScoreItem> dimensionAvgScores;

    /** 档案类型分布 */
    private List<StatisticsTypeCountItem> archiveTypeDistribution;
}
