package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private Integer stepNo;

    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_rule", length = 50)
    private String scopeRule;

    @Column(name = "auto_assign", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer autoAssign = 1;

    @Column(name = "allow_delegate", nullable = false, columnDefinition = "TINYINT")
    private Integer allowDelegate;

    @Column(name = "allow_skip", nullable = false, columnDefinition = "TINYINT")
    private Integer allowSkip;

    @Column(name = "allow_designate_next", nullable = false, columnDefinition = "TINYINT")
    private Integer allowDesignateNext;

    @Column(name = "timeout_hours", nullable = false, columnDefinition = "INT DEFAULT 48")
    private Integer timeoutHours = 48;

    @Column(name = "reject_action", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'end'")
    private String rejectAction = "end";

    @Column(name = "reject_to_step")
    private Integer rejectToStep;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
