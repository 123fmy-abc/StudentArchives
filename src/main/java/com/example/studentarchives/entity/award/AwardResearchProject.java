package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "award_research_projects")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AwardResearchProject extends BaseEntity {

    @Column(name = "research_star_id", nullable = false)
    private Long researchStarId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_level", length = 50)
    private String projectLevel;

    @Column(name = "rank_total", length = 50)
    private String rankTotal;

    @Column(name = "established_at")
    private LocalDate establishedAt;
}
