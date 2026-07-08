package com.example.studentarchives.entity.log;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_logs")
public class SystemLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "action")
    private String action;

    @Column(name = "module")
    private String module;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "log_level")
    private Integer logLevel;

    @Column(name = "is_display")
    private Integer isDisplay;

    @Column(name = "activity_name")
    private String activityName;

    @Column(name = "status")
    private Integer status;

    @Column(name = "status_label")
    private String statusLabel;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
