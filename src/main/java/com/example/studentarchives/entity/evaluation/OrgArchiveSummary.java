package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "org_archive_summaries")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class OrgArchiveSummary extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "org_type", nullable = false)
    private Integer orgType;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "total_students", nullable = false)
    private int totalStudents;

    @Column(name = "total_archives", nullable = false)
    private int totalArchives;

    @Column(name = "total_awards", nullable = false)
    private int totalAwards;

    @Column(name = "avg_gpa", precision = 3, scale = 2)
    private BigDecimal avgGpa;

    @Column(name = "top_dimensions", columnDefinition = "JSON")
    private String topDimensions;

    @Column(name = "hot_tags", columnDefinition = "JSON")
    private String hotTags;

    @Column(name = "archive_type_distribution", columnDefinition = "JSON")
    private String archiveTypeDistribution;
}
