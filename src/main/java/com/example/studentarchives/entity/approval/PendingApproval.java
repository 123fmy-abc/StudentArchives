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

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "instance_id")
    private Long instanceId;

    @Column(name = "approvable_type")
    private String approvableType;

    @Column(name = "approvable_id")
    private Long approvableId;

    @Column(name = "applicant_id")
    private Long applicantId;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "applicant_no")
    private String applicantNo;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category_label")
    private String categoryLabel;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "step_no")
    private Integer stepNo;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "status")
    private Integer status;
}
