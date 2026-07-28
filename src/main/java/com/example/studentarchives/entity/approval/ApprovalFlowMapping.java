package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "approval_flow_mappings")
public class ApprovalFlowMapping extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "business_type", length = 50, nullable = false)
    private String businessType;

    @Column(name = "business_sub_type", length = 50)
    private String businessSubType;

    @Column(name = "flow_id", nullable = false)
    private Long flowId;

    @Column(name = "is_default", nullable = false, columnDefinition = "TINYINT")
    private Integer isDefault;

    @Column(name = "effective_start")
    private LocalDateTime effectiveStart;

    @Column(name = "effective_end")
    private LocalDateTime effectiveEnd;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
