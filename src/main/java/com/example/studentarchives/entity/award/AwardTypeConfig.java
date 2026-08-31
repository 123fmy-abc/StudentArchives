package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "award_type_configs")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AwardTypeConfig extends BaseEntity {

    @Column(name = "award_type", nullable = false, length = 50)
    private String awardType;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Lob
    @Column(name = "evaluate_desc", columnDefinition = "TEXT")
    private String evaluateDesc;

    @Column(name = "evaluate_requirements", columnDefinition = "JSON")
    private String evaluateRequirements;

    @Column(name = "evaluate_notes", columnDefinition = "JSON")
    private String evaluateNotes;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}