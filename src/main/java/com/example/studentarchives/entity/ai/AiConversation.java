package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_conversations")
public class AiConversation extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private String title;

    @Column(columnDefinition = "JSON")
    private String context;

    private Integer status;
}
