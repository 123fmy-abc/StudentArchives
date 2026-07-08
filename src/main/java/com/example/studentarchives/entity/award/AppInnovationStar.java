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
@Table(name = "app_innovation_stars")
public class AppInnovationStar extends BaseEntity {

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "industry_type", nullable = false)
    private String industryType;

    @Column(name = "applicant_rank", nullable = false)
    private Integer applicantRank;

    @Column(name = "register_time")
    private LocalDate registerTime;
}
