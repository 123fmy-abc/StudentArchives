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
@Table(name = "user_behavior_logs")
public class UserBehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "behavior_type", length = 50, nullable = false)
    private String behaviorType;

    @Column(name = "target_page", length = 255, nullable = false)
    private String targetPage;

    @Column(name = "target_module", length = 100)
    private String targetModule;

    @Column(name = "meta", columnDefinition = "JSON")
    private String meta;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
