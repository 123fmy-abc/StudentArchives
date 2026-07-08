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
@Table(name = "app_software_copyrights")
public class AppSoftwareCopyright extends BaseEntity {

    @Column(name = "research_star_id", nullable = false)
    private Long researchStarId;

    @Column(name = "software_name", nullable = false, length = 255)
    private String softwareName;

    @Column(name = "issuing_unit", length = 255)
    private String issuingUnit;

    @Column(name = "rank_total", length = 50)
    private String rankTotal;

    @Column(name = "approval_time")
    private LocalDate approvalTime;
}
