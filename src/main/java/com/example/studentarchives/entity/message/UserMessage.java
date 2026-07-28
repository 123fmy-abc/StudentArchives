package com.example.studentarchives.entity.message;

import com.example.studentarchives.entity.BaseEntity;
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
@Table(name = "user_messages")
public class UserMessage extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "sender_type", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer senderType = 1;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_type", length = 100)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "send_channel", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'push'")
    private String sendChannel = "push";

    @Column(name = "is_read", nullable = false, columnDefinition = "TINYINT")
    private Integer isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_archived", nullable = false, columnDefinition = "TINYINT")
    private Integer isArchived;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "is_important", nullable = false, columnDefinition = "TINYINT")
    private Integer isImportant;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "jump_url", length = 500)
    private String jumpUrl;
}
