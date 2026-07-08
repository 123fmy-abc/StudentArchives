package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classes")
public class Clazz extends BaseEntity {

    @Column(name = "major_id")
    private Long majorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", insertable = false, updatable = false)
    private Major major;

    private String name;

    private String grade;

    @Column(name = "student_count")
    private Integer studentCount;

    private Integer status;
}
