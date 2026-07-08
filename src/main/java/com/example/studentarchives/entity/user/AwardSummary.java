package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "award_summaries")
public class AwardSummary extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "max_level", length = 50)
    private String maxLevel;

    @Column(name = "latest_time")
    private LocalDate latestTime;
}
