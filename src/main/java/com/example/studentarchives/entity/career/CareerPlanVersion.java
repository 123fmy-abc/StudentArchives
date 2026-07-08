package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "career_plan_versions")
public class CareerPlanVersion extends BaseEntity {

    private Long careerPlanId;

    private Integer version;

    private String title;

    @Column(columnDefinition = "JSON")
    private String dataSnapshot;

    private Integer status;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;
}
