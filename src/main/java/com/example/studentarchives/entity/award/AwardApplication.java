package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "award_applications")
@SQLRestriction("deleted_at IS NULL")
public class AwardApplication extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "award_type", nullable = false, length = 50)
    private String awardType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "certificate_no", length = 100)
    private String certificateNo;

    @Column(name = "issuing_unit", length = 255)
    private String issuingUnit;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "participant_role", length = 50)
    private String participantRole;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "passed_at")
    private LocalDateTime passedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "submit_count", nullable = false)
    private Integer submitCount;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
