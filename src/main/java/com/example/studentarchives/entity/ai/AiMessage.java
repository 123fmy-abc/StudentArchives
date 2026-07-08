package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_messages")
public class AiMessage extends BaseEntity {

    private Long conversationId;

    private String role;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String modelName;

    private Integer tokenUsage;

    private Integer generationTimeMs;
}
