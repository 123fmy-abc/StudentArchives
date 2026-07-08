package com.example.studentarchives.entity.approval;
import com.example.studentarchives.enums.StatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_flows")
public class ApprovalFlow extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "flow_name", nullable = false, length = 100)
    private String flowName;

    @Column(name = "applicable_type", nullable = false, length = 50)
    private String applicableType;

    @Column(name = "applicable_sub_type")
    private String applicableSubType;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "copied_from")
    private Long copiedFrom;

    @Column(name = "is_default", nullable = false)
    private byte isDefault;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusEnum status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
