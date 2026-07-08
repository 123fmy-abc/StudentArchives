package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "approval_delegations")
public class ApprovalDelegation extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "delegator_id")
    private Long delegatorId;

    @Column(name = "delegatee_id")
    private Long delegateeId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status")
    private Integer status;
}
