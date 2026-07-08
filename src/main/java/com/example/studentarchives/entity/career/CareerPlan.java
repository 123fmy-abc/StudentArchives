package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "career_plans")
public class CareerPlan extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private Integer currentVersion;

    private Integer submitCount;

    private Long semesterId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String requirement;

    private Integer status;

    private LocalDateTime submittedAt;

    private LocalDateTime auditedAt;

    private Long auditorId;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    private Long fileId;

    private LocalDateTime draftSavedAt;
}
