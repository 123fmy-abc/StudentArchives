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
@Table(name = "award_innovation_stars")
public class AwardInnovationStar extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "industry_type", nullable = false, length = 100)
    private String industryType;

    @Column(name = "applicant_rank", length = 50)
    private String applicantRank;

    @Column(name = "registered_at")
    private LocalDate registeredAt;
}
