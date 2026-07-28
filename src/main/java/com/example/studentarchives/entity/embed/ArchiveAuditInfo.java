package com.example.studentarchives.entity.embed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 档案/申请/规划的审核与提交流程公共字段
 * <p>
 * 用于 {@link com.example.studentarchives.entity.archive.Archive}、
 * {@link com.example.studentarchives.entity.award.AwardApplication}、
 * {@link com.example.studentarchives.entity.career.CareerPlan} 等实体中，
 * 避免重复定义相同的提交流程时间戳字段。
 */
@Getter
@Setter
@Embeddable
public class ArchiveAuditInfo {

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Lob
    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "passed_at")
    private LocalDateTime passedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "current_version")
    private Integer currentVersion;

    @Column(name = "submit_count")
    private Integer submitCount;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
