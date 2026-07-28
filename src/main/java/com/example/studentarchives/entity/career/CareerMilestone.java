package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "career_milestones")
@SQLRestriction("deleted_at IS NULL")
public class CareerMilestone extends BaseEntity {

    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @Column(name = "milestone_title", nullable = false, length = 255)
    private String milestoneTitle;

    @Column(name = "milestone_date")
    private LocalDate milestoneDate;

    @Column(name = "is_achieved", nullable = false)
    private Integer isAchieved;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @Column(name = "proof_file_id")
    private Long proofFileId;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
