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

    private Long weaknessId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestionContent;

    private Integer source;

    private Long teacherId;

    private Integer isImplemented;
}
