package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "award_competition_stars")
@SQLRestriction("deleted_at IS NULL")
public class AwardCompetitionStar extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "competition_name", nullable = false, length = 255)
    private String competitionName;

    @Column(name = "participated_at")
    private LocalDate participatedAt;

    @Column(name = "competition_level", nullable = false, length = 50)
    private String competitionLevel;

    @Column(name = "award_level", nullable = false, length = 50)
    private String awardLevel;
}
