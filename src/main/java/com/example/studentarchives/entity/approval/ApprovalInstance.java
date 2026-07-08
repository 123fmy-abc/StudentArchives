package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "approval_instances")
public class ApprovalInstance extends BaseEntity {

    @Column(name = "approvable_type", nullable = false, length = 100)
    private String approvableType;

    @Column(name = "approvable_id", nullable = false)
    private Long approvableId;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "flow_version", nullable = false)
    private int flowVersion;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "current_step", nullable = false)
    private byte currentStep;

    @Column(name = "total_steps", nullable = false)
    private byte totalSteps;

    @Column(name = "status", nullable = false)
    private byte status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
