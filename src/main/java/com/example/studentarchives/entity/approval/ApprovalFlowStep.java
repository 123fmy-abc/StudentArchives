package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_flow_steps")
public class ApprovalFlowStep extends BaseEntity {

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "step_no", nullable = false)
    private byte stepNo;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "scope_type", nullable = false)
    private byte scopeType;

    @Column(name = "scope_rule", nullable = false, length = 50)
    private String scopeRule;

    @Column(name = "auto_assign", nullable = false)
    private byte autoAssign;

    @Column(name = "allow_delegate", nullable = false)
    private byte allowDelegate;

    @Column(name = "allow_skip", nullable = false)
    private byte allowSkip;

    @Column(name = "allow_designate_next", nullable = false)
    private byte allowDesignateNext;

    @Column(name = "timeout_hours", nullable = false)
    private int timeoutHours;

    @Column(name = "reject_action", nullable = false, length = 20)
    private String rejectAction;

    @Column(name = "reject_to_step")
    private Byte rejectToStep;

    @Column(name = "sort", nullable = false)
    private int sort;
}
