package com.example.studentarchives.dto.Fmy.home.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页数据概览响应 DTO（GET /home/dashboard）
 * <p>
 * 数据来源（对应《学生端接口文档》3.1）：
 * users、student_profiles、classes、majors、semesters、
 * semester_gpa_summaries、portrait_evaluation_scores、
 * archives、user_messages、data_completeness、ability_dimensions 表聚合。
 * 所有数值必须由上述真实数据表计算/查询返回，禁止写死演示数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    /** 学生姓名 */
    private String studentName;

    /** 学号 */
    private String studentNo;

    /** 专业名称 */
    private String major;

    /** 班级名称 */
    private String className;

    /** 年级，如 "2024级" */
    private String grade;

    /** 当前日期，如 "2026年7月7日星期一" */
    private String currentDate;

    /** 申报总数（全部档案记录数） */
    private Integer applicationTotal;

    /** 已通过数（status=2） */
    private Integer approvedCount;

    /** 待审批数（status=1） */
    private Integer pendingCount;

    /** 已退回数（status=3） */
    private Integer rejectedCount;

    /** 当前学期加权平均绩点 */
    private BigDecimal currentGpa;

    /** 累计总学分（各学期 total_credit 求和） */
    private BigDecimal totalCredits;

    /** 当前学期班级排名 */
    private Integer rankInClass;

    /** 当前学期专业排名 */
    private Integer rankInMajor;

    /** 画像指标（当前学期各维度得分） */
    private List<IndicatorItem> indicators;

    /** 雷达图数据 */
    private RadarChart radarChart;

    /** 数据完整度 */
    private DataCompletenessInfo dataCompleteness;

    /** 快捷入口 */
    private List<QuickEntry> quickEntries;

    /** 最近动态 */
    private List<RecentActivity> recentActivities;

    /** 未读消息数 */
    private Long unreadMessageCount;

    // ==================== 嵌套结构 ====================

    /**
     * 画像指标项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorItem {

        /** 维度编码 */
        private String dimensionCode;

        /** 维度名称 */
        private String dimensionName;

        /** 当前得分 */
        private BigDecimal score;

        /** 较上阶段变化，如 "+7" */
        private String trend;

        /** 目标分 */
        private BigDecimal targetScore;

        /** 与目标差距 */
        private BigDecimal gap;

        /** 计量单位，如 "分" */
        private String unit;

        /** 对比学期 ID */
        private Long comparedSemesterId;

        /** 对比学期名称 */
        private String comparedSemesterName;

        /** 评分计算批次 ID */
        private Long calculationId;

        /** 指标规则版本 */
        private Integer ruleVersion;

        /** 评分时间（ISO 8601 带时区） */
        private String calculatedAt;
    }

    /**
     * 雷达图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RadarChart {

        /** 维度列表 */
        private List<DimensionInfo> dimensions;

        /** 当前学期各维度得分 */
        private List<BigDecimal> current;

        /** 目标分 */
        private List<BigDecimal> target;

        /** 上阶段各维度得分 */
        private List<BigDecimal> previous;
    }

    /**
     * 雷达图维度信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionInfo {

        private String code;

        private String name;
    }

    /**
     * 数据完整度
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataCompletenessInfo {

        /** 综合完整度（各维度完整度平均值） */
        private Integer rate;

        /** 缺失项列表（各维度缺失项汇总去重） */
        private List<String> missingItems;
    }

    /**
     * 快捷入口
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickEntry {

        /** 入口名称 */
        private String name;

        /** 图标标识 */
        private String icon;

        /** 前端路由 */
        private String path;

        /** 是否近期有相关动态（由真实档案数据判断） */
        private Boolean recent;
    }

    /**
     * 最近动态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {

        /** 档案 ID */
        private Long id;

        /** 标题 */
        private String title;

        /** 提交时间，如 "2026-06-28 14:30" */
        private String time;

        /** 动态类型：submitted/approved/rejected/draft */
        private String type;

        /** 档案类型编码 */
        private String archiveType;

        /** 申报状态（ApplyStatusEnum 值） */
        private Integer status;
    }
}
