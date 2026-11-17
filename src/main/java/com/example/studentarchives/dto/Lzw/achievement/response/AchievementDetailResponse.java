package com.example.studentarchives.dto.Lzw.achievement.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成果详情响应（GET /achievements/{id}）
 * <p>
 * 字段名与前端 AchievementDetail 一致（camelCase）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDetailResponse {

    private String id;
    private String title;
    private String category;
    private String categoryLabel;
    private String level;
    private String issuer;
    private String eventAt;
    private String description;

    /** 学期信息，未关联学期时为 null */
    private SemesterInfo semester;

    private String materialStatus;
    private String materialLabel;

    private List<AttachmentInfo> attachments;

    /** 详情补充字段：按「标签 - 值」展示 */
    private List<FieldInfo> fields;

    /** 审核信息，未进入审批流程时为 null */
    private AuditInfo audit;

    private String updatedAt;
    private List<String> tags;
    private Integer version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterInfo {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private String fileId;
        private String fileName;
        private String mimeType;
        private Long size;
        private String url;
        private String previewUrl;
        private String uploadedAt;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldInfo {
        private String label;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditInfo {
        private String status;
        private String statusLabel;
        private String reviewedAt;
        private String reviewerName;
    }
}
