package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "data_completeness")
public class DataCompleteness extends BaseEntityNoUpdate {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "dimension_code", nullable = false, length = 50)
    private String dimensionCode;

    @Column(name = "completeness_rate")
    private Integer completenessRate;

    @Column(name = "missing_items", columnDefinition = "JSON")
    private String missingItems;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}
