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
@Table(name = "app_published_papers")
public class AppPublishedPaper extends BaseEntity {

    @Column(name = "research_star_id", nullable = false)
    private Long researchStarId;

    @Column(name = "journal_name", nullable = false, length = 255)
    private String journalName;

    @Column(name = "paper_title", nullable = false, length = 255)
    private String paperTitle;

    @Column(name = "rank_total", length = 50)
    private String rankTotal;

    @Column(name = "publish_time")
    private LocalDate publishTime;
}
