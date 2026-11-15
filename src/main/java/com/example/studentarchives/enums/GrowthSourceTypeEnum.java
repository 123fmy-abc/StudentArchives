package com.example.studentarchives.enums;

/**
 * 成长时间轴来源记录类型
 * <p>
 * 与 {@code ProfileGrowthTimelineService#SOURCE_VALIDATION_SQL} 的键保持一致，
 * 用于计分规则（growth_source_score_rules）配置与展示标签。
 * 来源类型由学生关联的客观业务记录决定，不能任意填写。
 */
public enum GrowthSourceTypeEnum {

    ARCHIVES("archives", "学生档案"),
    AWARD_APPLICATIONS("award_applications", "奖项申报"),
    AWARD_COMPETITION_STARS("award_competition_stars", "竞赛星级"),
    SEMESTER_GPA_SUMMARIES("semester_gpa_summaries", "学期GPA汇总"),
    GPA_RECORDS("gpa_records", "GPA记录"),
    CAREER_PLANS("career_plans", "职业规划"),
    CAREER_PLAN_FEEDBACKS("career_plan_feedbacks", "职业规划反馈"),
    WEAKNESS_ANALYSES("weakness_analyses", "短板分析"),
    IMPROVEMENT_SUGGESTIONS("improvement_suggestions", "改进建议"),
    PORTRAIT_EVALUATION_SCORES("portrait_evaluation_scores", "画像评分"),
    ARCHIVE_COMPETITIONS("archive_competitions", "竞赛经历"),
    ARCHIVE_SOCIAL_PRACTICES("archive_social_practices", "社会实践"),
    ARCHIVE_INTERNSHIPS("archive_internships", "实习经历"),
    ARCHIVE_ORGANIZATIONS("archive_organizations", "组织任职");

    private final String code;
    private final String label;

    GrowthSourceTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 按编码查找，未知编码返回 null */
    public static GrowthSourceTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (GrowthSourceTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /** 编码 → 中文标签，未知编码返回原编码 */
    public static String labelOf(String code) {
        GrowthSourceTypeEnum type = of(code);
        return type != null ? type.label : code;
    }
}
