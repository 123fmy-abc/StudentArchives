package com.example.studentarchives.enums;

/**
 * 成长时间轴来源记录类型
 * <p>
 * 与 {@code ProfileGrowthTimelineService#SOURCE_VALIDATION_SQL} 的键保持一致，
 * 用于计分规则（growth_source_score_rules）配置与展示标签。
 * 来源类型由学生关联的客观业务记录决定，不能任意填写。
 * <p>
 * 每种来源类型固定映射一种成长事件类型（{@code growth_timelines.event_type}）：
 * 学生新增/修改时间轴事件时由后端按来源推导，不再接受前端传入的 eventType。
 * {@link #ARCHIVES} 覆盖全部档案子类型，需再按 {@code archives.archive_type} 细分，
 * 故其 {@code eventType} 为 {@code null}，由 {@code GrowthTimelineEventTypeResolver} 处理。
 */
public enum GrowthSourceTypeEnum {

    ARCHIVES("archives", "学生档案", null),
    AWARD_APPLICATIONS("award_applications", "奖项申报", EventTypeEnum.AWARD),
    AWARD_COMPETITION_STARS("award_competition_stars", "竞赛星级", EventTypeEnum.AWARD),
    SEMESTER_GPA_SUMMARIES("semester_gpa_summaries", "学期GPA汇总", EventTypeEnum.GRADE),
    GPA_RECORDS("gpa_records", "GPA记录", EventTypeEnum.GRADE),
    CAREER_PLANS("career_plans", "职业规划", EventTypeEnum.CAREER_PLAN),
    CAREER_PLAN_FEEDBACKS("career_plan_feedbacks", "职业规划反馈", EventTypeEnum.CAREER_PLAN),
    WEAKNESS_ANALYSES("weakness_analyses", "短板分析", EventTypeEnum.WEAKNESS_IMPROVE),
    IMPROVEMENT_SUGGESTIONS("improvement_suggestions", "改进建议", EventTypeEnum.WEAKNESS_IMPROVE),
    PORTRAIT_EVALUATION_SCORES("portrait_evaluation_scores", "画像评分", EventTypeEnum.ABILITY_UPGRADE),
    ARCHIVE_COMPETITIONS("archive_competitions", "竞赛经历", EventTypeEnum.AWARD),
    ARCHIVE_SOCIAL_PRACTICES("archive_social_practices", "社会实践", EventTypeEnum.PRACTICE),
    ARCHIVE_INTERNSHIPS("archive_internships", "实习经历", EventTypeEnum.PRACTICE),
    ARCHIVE_ORGANIZATIONS("archive_organizations", "组织任职", EventTypeEnum.PRACTICE);

    private final String code;
    private final String label;
    /** 该来源类型推导出的成长事件类型；通用档案（archives）为 null，按 archive_type 细分 */
    private final EventTypeEnum eventType;

    GrowthSourceTypeEnum(String code, String label, EventTypeEnum eventType) {
        this.code = code;
        this.label = label;
        this.eventType = eventType;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public EventTypeEnum getEventType() {
        return eventType;
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
