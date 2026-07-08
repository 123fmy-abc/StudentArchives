package com.example.studentarchives.entity.approval;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
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

    @Column(name = "approvable_type", nullable = false, length = 100)
    private String approvableType;

    @Column(name = "approvable_id", nullable = false)
    private Long approvableId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "applicant_no", nullable = false, length = 50)
    private String applicantNo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_label", nullable = false, length = 50)
    private String categoryLabel;

    @Column(name = "submit_time", nullable = false)
    private LocalDateTime submitTime;

    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "step_no", nullable = false)
    private byte stepNo;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "status", nullable = false)
    private byte status;
}
