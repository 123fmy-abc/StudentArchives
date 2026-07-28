package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "indicator_versions")
public class IndicatorVersion extends BaseEntityNoUpdate {

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "weight", precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "scoring_rule", columnDefinition = "JSON")
    private String scoringRule;

    @Column(name = "change_summary", length = 255)
    private String changeSummary;

    @Column(name = "created_by")
    private Long createdBy;
}
