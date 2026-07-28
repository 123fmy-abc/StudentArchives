package com.example.studentarchives.entity.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_type", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer loginType = 1;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "login_status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer loginStatus = 1;

    @Column(name = "fail_reason", length = 100)
    private String failReason;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
