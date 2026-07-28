package com.example.studentarchives.entity.approval;

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
@Table(name = "approval_instances")
public class ApprovalInstance extends BaseEntity {

    @Column(name = "approvable_type", length = 100, nullable = false)
    private String approvableType;

    @Column(name = "approvable_id", nullable = false)
    private Long approvableId;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "flow_version", nullable = false)
    private Integer flowVersion;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "current_step", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer currentStep = 1;

    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
