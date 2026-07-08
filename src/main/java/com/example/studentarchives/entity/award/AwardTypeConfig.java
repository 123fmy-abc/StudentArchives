package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "award_type_config")
public class AwardTypeConfig extends BaseEntity {

    @Column(name = "award_type", nullable = false)
    private String awardType;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "evaluate_desc", columnDefinition = "TEXT")
    private String evaluateDesc;

    @Column(name = "apply_desc", columnDefinition = "TEXT")
    private String applyDesc;

    @Column(name = "icon")
    private String icon;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "status")
    private Integer status;
}
