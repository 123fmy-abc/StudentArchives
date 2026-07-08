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

    @Column(name = "approvable_type")
    private String approvableType;

    @Column(name = "approvable_id")
    private Long approvableId;

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "flow_version")
    private Integer flowVersion;

    @Column(name = "applicant_id")
    private Long applicantId;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "total_steps")
    private Integer totalSteps;

    @Column(name = "status")
    private Integer status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
