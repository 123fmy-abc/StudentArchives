package com.example.studentarchives.entity.message;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notification_delivery_logs")
public class NotificationDeliveryLog extends BaseEntityNoUpdate {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;

    @Lob
    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "retry_count", nullable = false, columnDefinition = "TINYINT")
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false, columnDefinition = "TINYINT DEFAULT 3")
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "channel_message_id", length = 255)
    private String channelMessageId;
}
