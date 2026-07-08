package com.example.studentarchives.entity.grade;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "portrait_evaluation_scores")
public class PortraitEvaluationScore extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "dimension_code", nullable = false, length = 50)
    private String dimensionCode;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "target_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetScore;

    @Column(name = "change_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal changeValue;

    @Column(name = "gap", nullable = false, precision = 5, scale = 2)
    private BigDecimal gap;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
