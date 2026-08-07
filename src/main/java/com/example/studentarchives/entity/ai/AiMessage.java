package com.example.studentarchives.entity.ai;

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
@Table(name = "ai_messages")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AiMessage extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "generation_time_ms")
    private Integer generationTimeMs;

    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;
}
