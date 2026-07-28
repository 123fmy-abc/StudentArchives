package com.example.studentarchives.entity.user;

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
@Table(name = "roles")
@SQLRestriction("deleted_at IS NULL")
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "role_type", nullable = false)
    private Integer roleType;

    @Column(name = "is_system", nullable = false)
    private Byte isSystem;

    @Column(name = "is_auditor", nullable = false)
    private Byte isAuditor;

    @Column(name = "scope_types", columnDefinition = "JSON")
    private String scopeTypes;

    @Column(name = "max_scope_count", nullable = false)
    private Integer maxScopeCount;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
