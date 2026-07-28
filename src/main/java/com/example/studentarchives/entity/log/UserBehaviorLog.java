package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_behavior_logs")
public class UserBehaviorLog extends BaseEntityNoUpdate {

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
}
