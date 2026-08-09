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
@Table(name = "classes")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class Clazz extends BaseEntity {

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
