package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "political_status", length = 50)
    private String politicalStatus;

    @Column(name = "volunteer_hours", precision = 8, scale = 2)
    private BigDecimal volunteerHours;

    @Column(name = "language_ability", length = 255)
    private String languageAbility;

    @Column(name = "hobbies", length = 255)
    private String hobbies;
}
