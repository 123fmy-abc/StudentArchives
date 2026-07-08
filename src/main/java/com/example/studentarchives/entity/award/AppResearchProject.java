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
@Table(name = "app_research_projects")
public class AppResearchProject extends BaseEntity {

    @Column(name = "research_star_id")
    private Long researchStarId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "project_level", nullable = false)
    private String projectLevel;

    @Column(name = "rank_total", nullable = false)
    private Integer rankTotal;

    @Column(name = "establish_time")
    private LocalDate establishTime;
}
