package com.example.studentarchives.dto.Fmy.profile.response;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.enums.SensitiveType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 个人档案信息响应 DTO（GET /profile/info）
 * <p>
 * 数据来源（对应《学生端接口文档》4.1）：
 * users、student_profiles、classes、majors、colleges、
 * semester_gpa_summaries、portrait_evaluation_scores、
 * user_interests、award_summaries、weakness_analyses、user_contact_infos 表聚合。
 * 其中 academicInfo 为学籍同步信息，只读；contactInfo 来自 user_contact_infos；
 * totalVolunteerHours 为社会实践申报志愿时长累计汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileInfoResponse {

    /** 学籍基本信息（只读） */
    private AcademicInfo academicInfo;

    /** 个人联系信息（可修改） */
    private ContactInfo contactInfo;

    /** 志愿时长累计（小时） */
    private BigDecimal totalVolunteerHours;

    /** 画像分数（各能力维度得分） */
    private List<DimensionProfileItem> dimensionProfile;

    /** 兴趣标签列表 */
    private List<InterestItem> interests;

    /** 各学期成绩 */
    private List<SemesterGradeItem> semesterGrades;

    /** 个人奖项汇总 */
    private List<PersonalAwardItem> personalAwards;

    /** 短板分析列表 */
    private List<WeaknessItem> weaknessAnalysis;

    /** 自我评价（来自 student_profiles.self_evaluation） */
    private String selfEvaluation;

    // ==================== 嵌套结构 ====================

    /**
     * 学籍基本信息（来自 users / student_profiles / classes / majors / colleges）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicInfo {

        /** 用户 ID */
        private Long userId;

        /** 姓名 */
        private String name;

        /** 学号 */
        private String studentNo;

        /** 年级，如 "2024级" */
        private String grade;

        /** 专业名称 */
        private String major;

        /** 学历层次编码（ associate / undergraduate / master / doctor / postdoctor ） */
        private String degreeType;

        /** 学历层次展示名称 */
        private String degreeTypeLabel;

        /** 班级名称 */
        private String className;

        /** 学院名称 */
        private String collegeName;

        /** 性别（1=男 2=女） */
        private Integer gender;

        /** 性别标签 */
        private String genderLabel;

        /** 政治面貌（字典编码，如 party_member） */
        private String politicalStatus;

        /** 政治面貌展示名称（由字典表 political_status 解析，如 "中共党员"） */
        private String politicalStatusLabel;

        /** 学生状态编码（ current / fresh_graduate / graduated ） */
        private String studentStatus;

        /** 学生状态展示名称 */
        private String studentStatusLabel;

        /** 出生日期，如 "2005-03-15" */
        private String birthDate;
    }

    /**
     * 个人联系信息（来自 user_contact_infos）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {

        @Sensitive(SensitiveType.EMAIL)
        private String email;

        @Sensitive(SensitiveType.PHONE)
        private String phone;

        private String avatar;

        private String address;

        private String emergencyName;

        private String emergencyRelation;

        @Sensitive(SensitiveType.PHONE)
        private String emergencyPhone;
    }

    /**
     * 画像分数项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionProfileItem {

        /** 维度编码 */
        private String dimensionCode;

        /** 维度名称 */
        private String dimensionName;

        /** 当前得分 */
        private BigDecimal score;

        /** 目标分 */
        private BigDecimal targetScore;

        /** 与目标差距 */
        private BigDecimal gap;

        /** 评分计算批次 ID */
        private Long calculationId;

        /** 指标规则版本 */
        private Integer ruleVersion;

        /** 评分时间（ISO 8601 带时区） */
        private String calculatedAt;
    }

    /**
     * 兴趣标签项（来自 user_interests）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestItem {

        private Long id;

        /** 兴趣标签名 */
        private String tagName;

        /** 具体内容描述 */
        private String detailContent;

        /** 熟练度：1=入门 2=一般 3=熟练 4=精通 */
        private Integer proficiencyLevel;

        /** 熟练度标签 */
        private String proficiencyLabel;

        /** 权重/出现次数 */
        private Integer weight;

        /** 0=系统标签 1=用户手动添加 */
        private Integer isDetail;
    }

    /**
     * 学期成绩项（来自 semester_gpa_summaries + semesters）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterGradeItem {

        private Long semesterId;

        /** 学期编码，如 "2022-2023-1" */
        private String semester;

        /** 学期名称，如 "2022-2023第一学期" */
        private String semesterName;

        /** 课程数 */
        private Integer courseCount;

        /** 总学分 */
        private BigDecimal totalCredit;

        /** 加权绩点 */
        private BigDecimal gpa;

        /** 平均分 */
        private BigDecimal averageScore;

        /** 班级排名 */
        private Integer rankInClass;

        /** 专业排名 */
        private Integer rankInMajor;
    }

    /**
     * 个人奖项汇总项（来自 award_summaries）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalAwardItem {

        /** 奖项类别，如 "学科竞赛" */
        private String category;

        /** 获奖总数 */
        private Integer totalCount;

        /** 最高级别，如 "省级" */
        private String maxLevel;

        /** 最近一次获奖时间，如 "2025-09-01" */
        private String latestTime;
    }

    /**
     * 短板分析项（来自 weakness_analyses）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WeaknessItem {

        private Long id;

        /** 短板类型 */
        private String weaknessType;

        /** 短板描述 */
        private String weaknessDesc;

        /** 严重程度 1-5 */
        private Integer severityLevel;

        /** 0=未读 1=已读 */
        private Integer isRead;

        /** 创建时间（ISO 8601 带时区） */
        private String createdAt;

        /** 关联模型（archive/award_application/career_plan），无具体来源时为 null */
        private String relatedType;

        /** 关联记录 ID，无具体来源时为 null */
        private Long relatedId;
    }
}
