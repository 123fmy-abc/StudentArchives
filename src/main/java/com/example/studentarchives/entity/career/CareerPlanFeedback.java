package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "career_plan_feedbacks")
@SQLRestriction("deleted_at IS NULL")
public class CareerPlanFeedback extends BaseEntity {

    @Column(name = "career_plan_id", nullable = false)
    private Long careerPlanId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Lob
    @Column(name = "feedback_content", nullable = false, columnDefinition = "TEXT")
    private String feedbackContent;

    @Column(name = "suggestion_items", columnDefinition = "JSON")
    private String suggestionItems;
}
