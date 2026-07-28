package com.example.studentarchives.entity.grade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "grade_import_logs")
public class GradeImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "fail_details", columnDefinition = "JSON")
    private String failDetails;

    @Column(name = "import_status", nullable = false)
    private Integer importStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
