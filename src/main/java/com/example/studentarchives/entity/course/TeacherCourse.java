package com.example.studentarchives.entity.course;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 教师授课关系实体
 * 记录教师与授课班级、课程、学期的关联
 */
@Getter
@Setter
@Entity
@Table(name = "teacher_courses")
public class TeacherCourse extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "is_primary", nullable = false)
    private byte isPrimary;

    @Column(name = "status", nullable = false)
    private byte status;
}
