package com.example.studentarchives.entity.weakness;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "improvement_suggestions")
public class ImprovementSuggestion extends BaseEntity {

    @Column(name = "weakness_id", nullable = false)
    private Long weaknessId;

    @Lob
    @Column(name = "suggestion_content", nullable = false, columnDefinition = "TEXT")
    private String suggestionContent;

    @Column(name = "source", nullable = false)
    private byte source;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "is_implemented", nullable = false)
    private byte isImplemented;
}
