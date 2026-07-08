package com.example.studentarchives.entity.weakness;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "weakness_analyses")
public class WeaknessAnalysis extends BaseEntity {

    private Long userId;

    private String weaknessType;

    @Column(columnDefinition = "TEXT")
    private String weaknessDesc;

    private Integer source;

    private Long teacherId;

    private Integer severityLevel;

    private Integer targetScore;

    private Integer isRead;
}
