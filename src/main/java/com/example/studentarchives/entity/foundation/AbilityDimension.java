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
@Table(name = "ability_dimensions")
public class AbilityDimension extends BaseEntity {

    @Column(name = "dimension_name")
    private String dimensionName;

    @Column(name = "dimension_code")
    private String dimensionCode;

    private String description;

    private Integer sort;

    private Integer status;
}
