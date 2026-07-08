package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_flows")
public class ApprovalFlow extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "flow_name")
    private String flowName;

    @Column(name = "applicable_type")
    private String applicableType;

    @Column(name = "applicable_sub_type")
    private String applicableSubType;

    @Column(name = "version")
    private Integer version;

    @Column(name = "copied_from")
    private Long copiedFrom;

    @Column(name = "is_default")
    private Integer isDefault;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_by")
    private Long createdBy;
}
