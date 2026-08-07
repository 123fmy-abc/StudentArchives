package com.example.studentarchives.entity.ai;

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
@Table(name = "ai_conversations")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AiConversation extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "context", columnDefinition = "JSON")
    private String context;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;
}
