package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "approval_nodes")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ApprovalNode extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "assigned_auditor_id")
    private Long assignedAuditorId;

    @Column(name = "assign_type", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer assignType = 1;

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

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "timeout_at")
    private LocalDateTime timeoutAt;
}
