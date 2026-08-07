package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "career_goals")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class CareerGoal extends BaseEntity {

    @Column(name = "career_plan_id", nullable = false)
    private Long careerPlanId;

    @Column(name = "goal_title", nullable = false, length = 255)
    private String goalTitle;

    @Lob
    @Column(name = "goal_desc", columnDefinition = "TEXT")
    private String goalDesc;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "source", nullable = false)
    private Integer source = 1;

    @Column(name = "ai_suggestion_id")
    private Long aiSuggestionId;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
