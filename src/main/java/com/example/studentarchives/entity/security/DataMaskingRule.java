package com.example.studentarchives.entity.security;

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
@Table(name = "data_masking_rules")
@SQLRestriction("deleted_at IS NULL")
public class DataMaskingRule extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "target_table", nullable = false, length = 100)
    private String targetTable;

    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;

    @Column(name = "masking_strategy", nullable = false, length = 50)
    private String maskingStrategy;

    @Column(name = "masking_param", columnDefinition = "JSON")
    private String maskingParam;

    @Column(name = "apply_scenarios", columnDefinition = "JSON")
    private String applyScenarios;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
