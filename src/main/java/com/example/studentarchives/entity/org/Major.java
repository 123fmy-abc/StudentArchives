package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "majors")
public class Major extends BaseEntity {

    @Column(name = "college_id")
    private Long collegeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", insertable = false, updatable = false)
    private College college;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    private Integer status;
}
