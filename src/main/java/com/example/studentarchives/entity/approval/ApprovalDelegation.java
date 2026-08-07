package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "approval_delegations")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ApprovalDelegation extends BaseEntity {

    @Column(name = "school_id", nullable = false)
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

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
