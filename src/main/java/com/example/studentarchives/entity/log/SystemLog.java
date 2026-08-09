package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "system_logs")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class SystemLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", length = 100)
    private String roleName;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "module", length = 100, nullable = false)
    private String module;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "before_data", columnDefinition = "JSON")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "JSON")
    private String afterData;

    @Column(name = "log_level", nullable = false)
    private Integer logLevel;

    @Column(name = "is_deletable", nullable = false, columnDefinition = "TINYINT")
    private Integer isDeletable;

    @Column(name = "is_display", nullable = false, columnDefinition = "TINYINT")
    private Integer isDisplay;

    @Column(name = "activity_name", length = 255)
    private String activityName;

    @Column(name = "status")
    private Integer status;

    @Column(name = "status_label", length = 50)
    private String statusLabel;

    @Column(name = "related_type", length = 100)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;
}
