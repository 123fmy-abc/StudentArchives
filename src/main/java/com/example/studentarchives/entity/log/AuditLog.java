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

    @Column(name = "auditable_type", nullable = false, length = 100)
    private String auditableType;

    @Column(name = "auditable_id", nullable = false)
    private Long auditableId;

    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    @Column(name = "action", nullable = false)
    private byte action;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "version", nullable = false)
    private int version;
}
