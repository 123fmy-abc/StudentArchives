package com.example.studentarchives.entity.grade;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "gpa_records")
public class GpaRecord extends BaseEntityNoUpdate {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "course_type", length = 50)
    private String courseType;

    @Column(name = "attempt_no")
    private Byte attemptNo = 1;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "gpa", precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(name = "credit", precision = 3, scale = 1)
    private BigDecimal credit;
}
