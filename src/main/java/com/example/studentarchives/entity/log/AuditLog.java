package com.example.studentarchives.entity.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "auditable_type", length = 100, nullable = false)
    private String auditableType;

    @Column(name = "auditable_id", nullable = false)
    private Long auditableId;

    @Column(name = "auditor_id", nullable = false)
    private Long auditorId;

    @Column(name = "action", nullable = false)
    private Integer action;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Lob
    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    @Column(name = "revoked_log_id")
    private Long revokedLogId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_deletable", nullable = false, columnDefinition = "TINYINT")
    private Integer isDeletable;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
