package com.example.studentarchives.entity.org;
import com.example.studentarchives.enums.StatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.example.studentarchives.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "majors")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Major extends BaseEntity {

    @Column(name = "college_id", nullable = false)
    private Long collegeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", insertable = false, updatable = false)
    private College college;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusEnum status;
}
