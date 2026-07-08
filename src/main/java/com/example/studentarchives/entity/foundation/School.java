package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "schools")
public class School extends BaseEntity {

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    private Integer status;
}
