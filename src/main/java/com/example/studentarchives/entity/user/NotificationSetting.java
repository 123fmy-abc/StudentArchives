package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_settings")
public class NotificationSetting extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "email_enabled", nullable = false)
    private byte emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private byte smsEnabled;

    @Column(name = "push_enabled", nullable = false)
    private byte pushEnabled;
}
