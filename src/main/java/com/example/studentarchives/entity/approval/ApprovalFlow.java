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
@Table(name = "approval_flows")
public class ApprovalFlow extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "flow_name", length = 100, nullable = false)
    private String flowName;

    @Column(name = "applicable_type", length = 50, nullable = false)
    private String applicableType;

    @Column(name = "applicable_sub_type", length = 50)
    private String applicableSubType;

    @Column(name = "version", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer version = 1;

    @Column(name = "copied_from")
    private Long copiedFrom;

    @Column(name = "is_default", nullable = false, columnDefinition = "TINYINT")
    private Integer isDefault;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
