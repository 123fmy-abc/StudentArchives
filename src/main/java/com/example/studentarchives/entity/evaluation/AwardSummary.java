package com.example.studentarchives.entity.evaluation;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "award_summaries")
public class AwardSummary extends BaseEntityNoUpdate {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "max_level", length = 50)
    private String maxLevel;

    @Column(name = "latest_at")
    private LocalDate latestAt;
}
