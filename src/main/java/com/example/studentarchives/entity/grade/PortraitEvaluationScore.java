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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "dimension_code", nullable = false)
    private String dimensionCode;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "target_score", precision = 10, scale = 2)
    private BigDecimal targetScore;

    @Column(name = "`change`", precision = 10, scale = 2)
    private BigDecimal change;

    @Column(name = "gap", precision = 10, scale = 2)
    private BigDecimal gap;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;
}
