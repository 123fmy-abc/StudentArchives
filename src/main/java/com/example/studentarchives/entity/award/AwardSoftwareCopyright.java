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
@Table(name = "award_software_copyrights")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AwardSoftwareCopyright extends BaseEntity {

    @Column(name = "research_star_id", nullable = false)
    private Long researchStarId;

    @Column(name = "software_name", nullable = false, length = 255)
    private String softwareName;

    @Column(name = "rank_total", length = 50)
    private String rankTotal;

    @Column(name = "approved_at")
    private LocalDate approvedAt;
}
