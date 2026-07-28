package com.example.studentarchives.entity.weakness;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "improvement_suggestions")
@SQLRestriction("deleted_at IS NULL")
public class ImprovementSuggestion extends BaseEntity {

    @Column(name = "weakness_id")
    private Long weaknessId;

    @Column(name = "suggestion_type", length = 50)
    private String suggestionType;

    @Column(name = "related_goal_id")
    private Long relatedGoalId;

    @Lob
    @Column(name = "suggestion_content", nullable = false, columnDefinition = "TEXT")
    private String suggestionContent;

    @Column(name = "source", nullable = false)
    private Integer source = 1;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "is_implemented", nullable = false)
    private Integer isImplemented;
}
