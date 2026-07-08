package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "approval_nodes")
public class ApprovalNode extends BaseEntity {

    @Column(name = "instance_id")
    private Long instanceId;

    @Column(name = "step_no")
    private Integer stepNo;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "assigned_auditor_id")
    private Long assignedAuditorId;

    @Column(name = "assign_type")
    private Integer assignType;

    @Column(name = "actual_auditor_id")
    private Long actualAuditorId;

    @Column(name = "delegation_id")
    private Long delegationId;

    @Column(name = "action")
    private Integer action;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "next_node_id")
    private Long nextNodeId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "timeout_at")
    private LocalDateTime timeoutAt;
}
