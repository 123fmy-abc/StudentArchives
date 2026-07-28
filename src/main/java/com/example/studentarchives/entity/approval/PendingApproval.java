package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "pending_approvals")
public class PendingApproval extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "approvable_type", length = 100, nullable = false)
    private String approvableType;

    @Column(name = "approvable_id", nullable = false)
    private Long approvableId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "applicant_name", length = 100, nullable = false)
    private String applicantName;

    @Column(name = "applicant_no", length = 50, nullable = false)
    private String applicantNo;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_label", length = 50, nullable = false)
    private String categoryLabel;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;
}
