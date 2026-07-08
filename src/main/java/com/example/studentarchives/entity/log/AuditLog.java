package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "auditable_type")
    private String auditableType;

    @Column(name = "auditable_id")
    private Long auditableId;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "action")
    private Integer action;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "version")
    private Integer version;
}
