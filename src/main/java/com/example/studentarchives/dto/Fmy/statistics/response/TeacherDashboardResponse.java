package com.example.studentarchives.dto.Fmy.statistics.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 教师范围统计看板响应 DTO（GET /teacher/statistics/dashboard）
 * <p>
 * 数据来源：org_archive_summaries 快照（studentCount / averageGpa /
 * dimensionAvgScores / archiveTypeDistribution）+ archives / award_applications /
 * career_plans 审批状态计数（submitted / approved / pending / rejected）。
 * submittedCount = approvedCount + pendingCount + rejectedCount（排除草稿与已撤销）。
 * dimensionAvgScores / archiveTypeDistribution 复用 {@link DimensionAvgScoreItem} /
 * {@link StatisticsTypeCountItem}，与管理端 16.1/16.2 统计同源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherDashboardResponse {

    /** 学期 ID */
    private Long semesterId;

    /** 统计范围名称（如 "计科2301班"） */
    private String scopeName;

    /** 范围内学生数 */
    private Integer studentCount;

    /** 已提交申报数（审批状态 1/2/3 合计） */
    private Integer submittedCount;

    /** 已通过数（状态 2） */
    private Integer approvedCount;

    /** 待审批数（状态 1） */
    private Integer pendingCount;

    /** 已退回数（状态 3） */
    private Integer rejectedCount;

    /** 平均绩点 */
    private Double averageGpa;

    /** 各能力维度平均分 */
    private List<DimensionAvgScoreItem> dimensionAvgScores;

    /** 档案类型分布 */
    private List<StatisticsTypeCountItem> archiveTypeDistribution;

    /** 快照命中标记（响应头 X-Cache-Hit 使用；L2=命中统计缓存 MISS=读快照/实时聚合） */
    private Boolean cacheHit;
}
