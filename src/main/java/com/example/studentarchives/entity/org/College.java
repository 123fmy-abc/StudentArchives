package com.example.studentarchives.entity.org;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.foundation.School;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "colleges")
public class College extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School school;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    private Integer status;
}
