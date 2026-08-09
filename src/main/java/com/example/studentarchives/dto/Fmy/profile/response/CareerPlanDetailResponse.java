package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 职业规划详情响应 DTO（GET /profile/career-plans/{planId}）
 * <p>
 * 数据来源：career_plans、career_goals、career_actions、career_milestones、
 * file_uploads（biz_type='career_plan'）、career_reflections、
 * career_plan_feedbacks、model_versions。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanDetailResponse {

    private Long id;

    private Long semesterId;

    private String semesterName;

    private String title;

    private String content;

    private String requirement;

    private Integer progressRate;

    private Integer status;

    private String statusLabel;

    private String submittedAt;

    private String auditedAt;

    private String auditorName;

    private String rejectedReason;

    private Long copyFromId;

    /** 1=手动创建 2=AI建议添加 */
    private Integer source;

    private String sourceLabel;

    private Long aiSuggestionId;

    private Integer requireConfirm;

    private List<GoalDetail> goals;

    private List<ReflectionItem> reflections;

    private List<FeedbackItem> feedbacks;

    private List<VersionItem> versionHistory;

    /**
     * 目标详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalDetail {

        private Long id;

        private String goalTitle;

        private String goalDesc;

        /** 目标日期，如 "2025-12-31" */
        private String targetDate;

        /** 0=未开始 1=进行中 2=已完成 */
        private Integer status;

        private String statusLabel;

        private List<ActionDetail> actions;
    }

    /**
     * 行动详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDetail {

        private Long id;

        private String actionTitle;

        private String actionDesc;

        private Integer status;

        private String statusLabel;

        private String startDate;

        private String endDate;

        private Integer completionRate;

        private List<FileItem> files;

        private List<MilestoneItem> milestones;
    }

    /**
     * 文件项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileItem {

        private Long fileId;

        private String fileName;

        private String fileUrl;
    }

    /**
     * 里程碑项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneItem {

        private Long id;

        private String milestoneTitle;

        private String milestoneDate;

        /** 0=未达成 1=已达成 */
        private Integer isAchieved;

        /** 成果证明材料（关联 file_uploads.id），未绑定为 null */
        private Long proofFileId;

        private String proofFileName;

        private String proofFileUrl;
    }

    /**
     * 阶段反思项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReflectionItem {

        private Long id;

        private String reflectionContent;

        /** 创建时间（ISO 8601 带时区） */
        private String createdAt;
    }

    /**
     * 教师反馈项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackItem {

        private Long id;

        private String teacherName;

        private String feedbackContent;

        private String createdAt;
    }

    /**
     * 版本历史项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionItem {

        private Integer version;

        private Integer status;

        private String statusLabel;

        /** 创建时间（ISO 8601 带时区） */
        private String createdAt;
    }
}
