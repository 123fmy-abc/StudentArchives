package com.example.studentarchives.dto.Lzw.activity.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 动态记录详情响应（GET /activities/{type}/{activityId}）
 * <p>
 * 聚合三张源表（archives / award_applications / career_plans）的完整字段。
 * 仅填充当前 type 相关的字段，其余为 null 并由 @JsonInclude(NON_NULL) 过滤。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDetailResponse {

    // ==================== 公共字段 ====================

    private Long id;
    private String type;
    private String title;
    private Integer status;

    @JsonProperty("status_label")
    private String statusLabel;

    @JsonProperty("semester_id")
    private Long semesterId;

    @JsonProperty("semester_name")
    private String semesterName;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    // ==================== Archive 专用 ====================

    @JsonProperty("archive_type")
    private String archiveType;

    @JsonProperty("archive_type_label")
    private String archiveTypeLabel;

    @JsonProperty("course_code")
    private String courseCode;

    @JsonProperty("obtained_at")
    private String obtainedAt;

    @JsonProperty("duplicate_check_status")
    private Integer duplicateCheckStatus;

    @JsonProperty("duplicate_info")
    private String duplicateInfo;

    @JsonProperty("correction_reason")
    private String correctionReason;

    // ==================== Award 专用 ====================

    @JsonProperty("award_type")
    private String awardType;

    @JsonProperty("certificate_no")
    private String certificateNo;

    @JsonProperty("issuing_unit")
    private String issuingUnit;

    @JsonProperty("valid_until")
    private String validUntil;

    @JsonProperty("participant_role")
    private String participantRole;

    // ==================== CareerPlan 专用 ====================

    private String content;
    private String requirement;

    @JsonProperty("progress_rate")
    private Integer progressRate;

    private Integer source;

    @JsonProperty("ai_suggestion_id")
    private Long aiSuggestionId;

    @JsonProperty("require_confirm")
    private Integer requireConfirm;

    // ==================== 审核流程字段（来自 AuditInfo）====================

    @JsonProperty("submit_time")
    private String submitTime;

    @JsonProperty("audited_at")
    private String auditedAt;

    @JsonProperty("auditor_id")
    private Long auditorId;

    @JsonProperty("auditor_name")
    private String auditorName;

    @JsonProperty("rejected_reason")
    private String rejectedReason;

    @JsonProperty("returned_at")
    private String returnedAt;

    @JsonProperty("passed_at")
    private String passedAt;

    @JsonProperty("revoked_at")
    private String revokedAt;

    @JsonProperty("current_version")
    private Integer currentVersion;

    @JsonProperty("submit_count")
    private Integer submitCount;

    // ==================== 嵌套列表 ====================

    /** 佐证材料 */
    @JsonProperty("evidence_files")
    private List<FileItem> evidenceFiles;

    /** 审批历史 */
    @JsonProperty("approval_history")
    private List<ApprovalHistoryItem> approvalHistory;

    /** 版本历史 */
    @JsonProperty("version_history")
    private List<VersionHistoryItem> versionHistory;

    // ==================== 内嵌类 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileItem {
        @JsonProperty("file_id")
        private Long fileId;

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("file_url")
        private String fileUrl;

        @JsonProperty("file_size")
        private Long fileSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApprovalHistoryItem {
        @JsonProperty("step_no")
        private Integer stepNo;

        @JsonProperty("step_name")
        private String stepName;

        @JsonProperty("auditor_name")
        private String auditorName;

        private Integer action;

        @JsonProperty("action_label")
        private String actionLabel;

        private String comment;

        @JsonProperty("completed_at")
        private String completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VersionHistoryItem {
        private Integer version;
        private String title;
        private Integer status;

        @JsonProperty("status_label")
        private String statusLabel;

        @JsonProperty("created_at")
        private String createdAt;
    }
}
