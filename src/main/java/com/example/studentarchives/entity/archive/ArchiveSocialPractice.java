package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "archive_social_practices")
public class ArchiveSocialPractice extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "activity_name", nullable = false, length = 255)
    private String activityName;

    @Column(name = "practice_location", length = 255)
    private String practiceLocation;

    @Column(name = "practice_unit", length = 255)
    private String practiceUnit;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "volunteer_hours", precision = 8, scale = 2)
    private BigDecimal volunteerHours;
}
