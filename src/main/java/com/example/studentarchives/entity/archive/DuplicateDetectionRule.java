package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "duplicate_detection_rules")
@SQLRestriction("deleted_at IS NULL")
public class DuplicateDetectionRule extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "detect_fields", nullable = false, columnDefinition = "JSON")
    private String detectFields;

    @Column(name = "similarity_algorithm", nullable = false, length = 50)
    private String similarityAlgorithm = "exact";

    @Column(name = "similarity_threshold", nullable = false, precision = 3, scale = 2)
    private BigDecimal similarityThreshold = new BigDecimal("1.00");

    @Column(name = "time_window_days")
    private Integer timeWindowDays;

    @Column(name = "auto_check", nullable = false)
    private Integer autoCheck = 1;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
