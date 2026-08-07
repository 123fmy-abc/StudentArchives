package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "career_reflections")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class CareerReflection extends BaseEntity {

    @Column(name = "career_plan_id", nullable = false)
    private Long careerPlanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Lob
    @Column(name = "reflection_content", nullable = false, columnDefinition = "TEXT")
    private String reflectionContent;
}
