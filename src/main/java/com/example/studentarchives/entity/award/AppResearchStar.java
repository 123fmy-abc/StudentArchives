package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_research_stars")
public class AppResearchStar extends BaseEntity {

    @Column(name = "application_id")
    private Long applicationId;
}
