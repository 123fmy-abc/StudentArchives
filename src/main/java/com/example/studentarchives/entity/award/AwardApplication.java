package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "award_applications")
public class AwardApplication extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "award_type", nullable = false)
    private String awardType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "current_version")
    private Integer currentVersion;

    @Column(name = "submit_count")
    private Integer submitCount;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
