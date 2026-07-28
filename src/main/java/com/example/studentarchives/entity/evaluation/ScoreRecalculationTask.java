package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "score_recalculation_tasks")
public class ScoreRecalculationTask extends BaseEntityNoUpdate {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "task_type", nullable = false)
    private Integer taskType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "status")
    private byte status = 0;

    @Column(name = "triggered_by", nullable = false)
    private Long triggeredBy;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_count")
    private int totalCount;

    @Column(name = "success_count")
    private int successCount;

    @Column(name = "fail_count")
    private int failCount;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Lob
    private String errorMessage;

    @Column(name = "lock_version", nullable = false)
    private int lockVersion;
}
