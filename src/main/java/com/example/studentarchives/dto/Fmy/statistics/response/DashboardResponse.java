package com.example.studentarchives.dto.Fmy.statistics.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学校整体档案汇总 DTO（GET /admin/statistics/dashboard，文档 16.1）
 * <p>
 * 统计看板首屏 KPI 与多维概览（成绩、奖项、实践、爱好）。
 * 数据优先来自 statistics_cache / org_archive_summaries 学校级快照；
 * 全校聚合缓存未命中时返回空数据集并标记 cacheHit=false，避免慢 SQL 打库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    /** 学生总数 */
    private Integer studentCount;

    /** 档案总数 */
    private Integer archiveCount;

    /** 获奖总数 */
    private Integer awardCount;

    /** 平均绩点 */
    private Double avgGpa;

    /** 已通过档案数 */
    private Integer approvedCount;

    /** 待审批档案数 */
    private Integer pendingCount;

    /** 数据完整度（0-100） */
    private Double dataCompleteness;

    /** 画像维度平均分 */
    private List<DimensionAvgScoreItem> dimensionAvgScores;

    /** 档案类型分布 */
    private List<StatisticsTypeCountItem> archiveTypeDistribution;

    /** 热门兴趣 TopN */
    private List<TopInterestItem> topInterests;

    /** 缓存命中标记：true=命中缓存/快照，false=全校范围聚合缓存未命中返回空集 */
    private Boolean cacheHit;
}
