package com.example.studentarchives.entity.grade;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "gpa_records")
public class GpaRecord extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "attempt_no")
    private Integer attemptNo;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "gpa", precision = 4, scale = 2)
    private BigDecimal gpa;

    @Column(name = "credit", precision = 5, scale = 2)
    private BigDecimal credit;
}
