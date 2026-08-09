package com.example.studentarchives.entity.message;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "notification_settings")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class NotificationSetting extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "email_enabled", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer emailEnabled = 1;

    @Column(name = "sms_enabled", nullable = false, columnDefinition = "TINYINT")
    private Integer smsEnabled;

    @Column(name = "push_enabled", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer pushEnabled = 1;
}
