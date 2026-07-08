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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "weakness_type", nullable = false, length = 100)
    private String weaknessType;

    @Lob
    @Column(name = "weakness_desc", columnDefinition = "TEXT")
    private String weaknessDesc;

    @Column(name = "source", nullable = false)
    private byte source;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "severity_level", nullable = false)
    private byte severityLevel;

    @Column(name = "target_score")
    private Integer targetScore;

    @Column(name = "is_read", nullable = false)
    private byte isRead;
}
