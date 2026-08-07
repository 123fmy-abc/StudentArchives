package com.example.studentarchives.entity.security;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "data_security_policies")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class DataSecurityPolicy extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "policy_config", columnDefinition = "JSON")
    private String policyConfig;

    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
