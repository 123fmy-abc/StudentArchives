package com.example.studentarchives.entity.schedule;

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
@Table(name = "scheduled_tasks")
public class ScheduledTask extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "task_name", length = 100, nullable = false)
    private String taskName;

    @Column(name = "task_code", length = 50, nullable = false)
    private String taskCode;

    @Column(name = "task_group", length = 50, nullable = false)
    private String taskGroup;

    @Column(name = "cron_expression", length = 100, nullable = false)
    private String cronExpression;

    @Column(name = "task_handler", length = 255, nullable = false)
    private String taskHandler;

    @Column(name = "task_params", columnDefinition = "JSON")
    private String taskParams;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system", nullable = false, columnDefinition = "TINYINT")
    private Integer isSystem;

    @Column(name = "run_type", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer runType = 1;

    @Column(name = "max_retries", nullable = false, columnDefinition = "TINYINT")
    private Integer maxRetries;

    @Column(name = "retry_delay_sec", nullable = false, columnDefinition = "INT DEFAULT 60")
    private Integer retryDelaySec = 60;

    @Column(name = "timeout_sec", nullable = false)
    private Integer timeoutSec;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_run_status")
    private Integer lastRunStatus;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
