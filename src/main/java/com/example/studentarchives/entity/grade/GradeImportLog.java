package com.example.studentarchives.entity.grade;

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
@Table(name = "grade_import_logs")
public class GradeImportLog extends BaseEntity {

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "file_id", nullable = false)
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
    private byte importStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
