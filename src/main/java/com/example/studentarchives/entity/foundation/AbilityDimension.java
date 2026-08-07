package com.example.studentarchives.entity.foundation;

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
@Table(name = "ability_dimensions")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AbilityDimension extends BaseEntity {

    @Column(name = "dimension_name", nullable = false, length = 50)
    private String dimensionName;

    @Column(name = "dimension_code", nullable = false, length = 50)
    private String dimensionCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
