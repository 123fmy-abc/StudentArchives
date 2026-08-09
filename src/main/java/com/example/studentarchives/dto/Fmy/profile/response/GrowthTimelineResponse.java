package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成长时间轴响应 DTO（GET /profile/growth-timeline，viewType=list）
 * <p>
 * 数据来源：growth_timelines、growth_timeline_abilities、growth_timeline_tags。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrowthTimelineResponse {

    /** 汇总信息 */
    private Summary summary;

    /** 时间线节点列表（viewType=list，按 eventAt 倒序） */
    private List<TimelineItem> timeline;

    /** 学年分组树（viewType=tree） */
    private List<YearGroup> tree;

    /** 年度聚合环形数据（viewType=ring） */
    private List<RingYear> ring;

    /**
     * 汇总信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {

        /** 经历总数 */
        private Integer experiences;

        /** 能力/技能数 */
        private Integer skills;

        /** 平均成长率：画像评估各维度 change/上阶段分数 均值百分比，如 "7%"；无数据为 "0%" */
        private String averageGrowth;

        /** 潜力/提升空间：画像评估各维度 gap/目标分 均值百分比，如 "12%"；无数据为 "0%" */
        private String potential;
    }

    /**
     * 时间线节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineItem {

        private Long id;

        private Long semesterId;

        /** 学期名称 */
        private String semesterName;

        /** 事件日期，如 "2025-07-05" */
        private String eventAt;

        private String eventName;

        private String content;

        /** 事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升 */
        private Integer eventType;

        private String eventTypeLabel;

        /** 审核状态：0=草稿 1=待审核 2=已通过 3=已退回 4=已撤销 */
        private Integer status;

        private String statusLabel;

        private String coverImage;

        private Long sourceId;

        private String sourceType;

        /** 能力得分数据 */
        private List<AbilityItem> abilityData;

        /** 标签列表 */
        private List<String> tags;
    }

    /**
     * 能力得分项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityItem {

        private String dimensionCode;

        private String dimensionName;

        private BigDecimal score;
    }

    /**
     * 学年分组（viewType=tree）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearGroup {

        /** 学年，如 "2023-2024" */
        private String academicYear;

        /** 该学年下的学期分组（按学期时间倒序） */
        private List<SemesterGroup> semesters;
    }

    /**
     * 学期分组（viewType=tree）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterGroup {

        private Long semesterId;

        private String semesterName;

        /** 该学期下的事件（按 eventAt 倒序） */
        private List<TimelineItem> events;
    }

    /**
     * 年度聚合节点（viewType=ring）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RingYear {

        /** 年度，如 "2023"（eventAt 所在自然年） */
        private String year;

        /** 该年度事件总数 */
        private Integer eventCount;

        /** 事件类型分布：eventType → 数量 */
        private Map<String, Integer> typeDistribution;

        /** 该年度下的事件（按 eventAt 倒序） */
        private List<TimelineItem> events;
    }
}
