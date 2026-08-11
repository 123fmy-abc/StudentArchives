package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "evaluation_indicators")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class EvaluationIndicator extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "path", length = 255)
    private String path;

    @Column(name = "weight", precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "scoring_rule", columnDefinition = "JSON")
    private String scoringRule;

    @Column(name = "dimension_code", length = 50)
    private String dimensionCode;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
