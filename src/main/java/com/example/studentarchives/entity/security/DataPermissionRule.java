package com.example.studentarchives.entity.security;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "data_permission_rules")
public class DataPermissionRule extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "target_table", nullable = false, length = 100)
    private String targetTable;

    @Column(name = "target_fields", columnDefinition = "JSON")
    private String targetFields;

    @Column(name = "filter_rule", columnDefinition = "JSON")
    private String filterRule;

    @Column(name = "scope_relation", length = 20)
    private String scopeRelation;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
