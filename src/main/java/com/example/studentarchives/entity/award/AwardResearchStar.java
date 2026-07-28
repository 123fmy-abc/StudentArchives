package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "award_research_stars")
@SQLRestriction("deleted_at IS NULL")
public class AwardResearchStar extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "primary_category", nullable = false, length = 50)
    private String primaryCategory;
}
