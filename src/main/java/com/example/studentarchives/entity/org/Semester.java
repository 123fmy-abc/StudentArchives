package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "semesters")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class Semester extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    private Integer isCurrent;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
