package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "college_id")
    private Long collegeId;

    @Column(name = "title", length = 100)
    private String title;
}
