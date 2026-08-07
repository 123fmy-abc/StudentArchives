package com.example.studentarchives.entity.growth;

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
@Table(name = "growth_timeline_abilities")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class GrowthTimelineAbility extends BaseEntity {

    @Column(name = "timeline_id", nullable = false)
    private Long timelineId;

    @Column(name = "dimension_code", length = 50, nullable = false)
    private String dimensionCode;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;
}
