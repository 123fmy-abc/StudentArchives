package com.example.studentarchives.entity.archive;
import com.example.studentarchives.enums.ApplyStatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "archives")
public class Archive extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "obtain_time")
    private LocalDate obtainTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private ApplyStatusEnum status;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "audited_at")
    private LocalDateTime auditedAt;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "submit_count", nullable = false)
    private int submitCount;

    @Column(name = "draft_saved_at")
    private LocalDateTime draftSavedAt;
}
