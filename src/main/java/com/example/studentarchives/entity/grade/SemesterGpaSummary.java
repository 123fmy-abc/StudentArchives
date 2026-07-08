package com.example.studentarchives.entity.grade;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "semester_gpa_summaries")
public class SemesterGpaSummary extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "course_count", nullable = false)
    private int courseCount;

    @Column(name = "total_credit", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalCredit;

    @Column(name = "weighted_gpa", nullable = false, precision = 3, scale = 2)
    private BigDecimal weightedGpa;

    @Column(name = "average_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "rank_in_major")
    private Integer rankInMajor;
}
