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

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "step_no")
    private Integer stepNo;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_rule")
    private String scopeRule;

    @Column(name = "auto_assign")
    private Integer autoAssign;

    @Column(name = "allow_delegate")
    private Integer allowDelegate;

    @Column(name = "allow_skip")
    private Integer allowSkip;

    @Column(name = "allow_designate_next")
    private Integer allowDesignateNext;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    @Column(name = "reject_action")
    private String rejectAction;

    @Column(name = "reject_to_step")
    private Integer rejectToStep;

    @Column(name = "sort")
    private Integer sort;
}
