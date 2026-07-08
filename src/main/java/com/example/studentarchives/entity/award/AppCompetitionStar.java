package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "app_competition_stars")
public class AppCompetitionStar extends BaseEntity {

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "competition_name", nullable = false)
    private String competitionName;

    @Column(name = "participate_time")
    private LocalDate participateTime;

    @Column(name = "competition_level", nullable = false)
    private String competitionLevel;

    @Column(name = "award_level", nullable = false)
    private String awardLevel;
}
