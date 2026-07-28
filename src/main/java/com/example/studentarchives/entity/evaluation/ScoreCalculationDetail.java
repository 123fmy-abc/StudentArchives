package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "score_calculation_details")
public class ScoreCalculationDetail extends BaseEntityNoUpdate {

    @Column(name = "calculation_id", nullable = false)
    private Long calculationId;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "dimension_code", nullable = false, length = 50)
    private String dimensionCode;

    @Column(name = "raw_score", precision = 5, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "weight", precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "weighted_score", precision = 5, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "source_archive_ids", columnDefinition = "JSON")
    private String sourceArchiveIds;
}
