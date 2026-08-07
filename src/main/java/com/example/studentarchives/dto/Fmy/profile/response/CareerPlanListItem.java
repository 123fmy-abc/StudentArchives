package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职业规划列表项 DTO（GET /profile/career-plans 的 list 元素）
 * <p>
 * 数据来源：career_plans 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanListItem {

    private Long id;

    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    private String title;

    /** 整体进度 0-100 */
    private Integer progressRate;

    /** 0=草稿 1=待审批 2=已通过 3=已退回 4=已撤销 */
    private Integer status;

    private String statusLabel;

    /** 提交时间（ISO 8601 带时区） */
    private String submittedAt;

    /** 审核时间（ISO 8601 带时区） */
    private String auditedAt;

    /** 当前版本号 */
    private Integer currentVersion;

    /** 提交次数 */
    private Integer submitCount;

    private String rejectedReason;

    /** 审核教师姓名 */
    private String auditorName;
}
