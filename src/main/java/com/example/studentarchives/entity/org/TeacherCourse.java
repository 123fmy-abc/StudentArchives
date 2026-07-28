package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "teacher_courses")
@SQLRestriction("deleted_at IS NULL")
public class TeacherCourse extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "course_name", length = 255)
    private String courseName;

    @Column(name = "is_primary")
    private Byte isPrimary;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
