package com.example.studentarchives.entity.career;
import com.example.studentarchives.enums.ApplyStatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "career_plans")
public class CareerPlan extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "submit_count", nullable = false)
    private int submitCount;

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

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private ApplyStatusEnum status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
