package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "career_plans")
@SQLRestriction("deleted_at IS NULL")
public class CareerPlan extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "submit_count", nullable = false)
    private Integer submitCount;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "copy_from_id")
    private Long copyFromId;

    @Column(name = "source", nullable = false)
    private Integer source = 1;

    @Column(name = "ai_suggestion_id")
    private Long aiSuggestionId;

    @Column(name = "require_confirm", nullable = false)
    private Integer requireConfirm = 1;

    @Column(name = "progress_rate", nullable = false)
    private Integer progressRate;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "passed_at")
    private LocalDateTime passedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
