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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "email_enabled")
    private Integer emailEnabled;

    @Column(name = "sms_enabled")
    private Integer smsEnabled;

    @Column(name = "push_enabled")
    private Integer pushEnabled;
}
