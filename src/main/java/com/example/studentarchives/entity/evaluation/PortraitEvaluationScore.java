package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "portrait_evaluation_scores")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class PortraitEvaluationScore extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "calculation_id")
    private Long calculationId;

    @Column(name = "dimension_code", nullable = false, length = 50)
    private String dimensionCode;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "target_score", precision = 5, scale = 2)
    private BigDecimal targetScore;

    @Column(name = "change", precision = 5, scale = 2)
    private BigDecimal changeVal;

    @Column(name = "gap", precision = 5, scale = 2)
    private BigDecimal gap;

    @Column(name = "compared_semester_id")
    private Long comparedSemesterId;

    @Column(name = "rule_version", nullable = false)
    private int ruleVersion;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
