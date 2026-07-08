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

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "fail_count")
    private Integer failCount;

    @Column(name = "fail_details", columnDefinition = "JSON")
    private String failDetails;

    @Column(name = "import_status")
    private Integer importStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
