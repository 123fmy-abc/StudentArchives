package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "career_actions")
public class CareerAction extends BaseEntity {

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(name = "action_title", nullable = false, length = 255)
    private String actionTitle;

    @Lob
    @Column(name = "action_desc", columnDefinition = "TEXT")
    private String actionDesc;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "source", nullable = false)
    private Integer source = 1;

    @Column(name = "ai_suggestion_id")
    private Long aiSuggestionId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "completion_rate", nullable = false)
    private Integer completionRate;

    @Column(name = "sort", nullable = false)
    private Integer sort;
}
