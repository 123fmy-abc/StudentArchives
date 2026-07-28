package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "award_type_configs")
public class AwardTypeConfig extends BaseEntity {

    @Column(name = "award_type", nullable = false, length = 50)
    private String awardType;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Lob
    @Column(name = "evaluate_desc", columnDefinition = "TEXT")
    private String evaluateDesc;


    @Lob
    @Column(name = "apply_desc", columnDefinition = "TEXT")
    private String applyDesc;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
