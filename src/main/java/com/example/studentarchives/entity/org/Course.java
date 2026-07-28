package com.example.studentarchives.entity.org;

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
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "college_id")
    private Long collegeId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "credit", precision = 3, scale = 1)
    private BigDecimal credit;

    @Column(name = "course_type", length = 50)
    private String courseType;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
