package com.example.studentarchives.entity.message;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_messages")
public class UserMessage extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "sender_type", nullable = false)
    private byte senderType;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "send_channel", nullable = false, length = 20)
    private String sendChannel;

    @Column(name = "is_read", nullable = false)
    private byte isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
