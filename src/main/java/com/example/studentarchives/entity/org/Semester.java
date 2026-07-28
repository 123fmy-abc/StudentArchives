package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "semesters")
@SQLRestriction("deleted_at IS NULL")
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
    private Byte isCurrent;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
