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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_count")
    private Integer courseCount;

    @Column(name = "total_credit", precision = 10, scale = 2)
    private BigDecimal totalCredit;

    @Column(name = "weighted_gpa", precision = 4, scale = 2)
    private BigDecimal weightedGpa;

    @Column(name = "average_score", precision = 10, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "rank_in_major")
    private Integer rankInMajor;
}
