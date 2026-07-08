package com.example.studentarchives.entity.user;
import com.example.studentarchives.enums.StatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "level", nullable = false)
    private byte level;

    @Column(name = "is_system", nullable = false)
    private byte isSystem;

    @Column(name = "is_auditor", nullable = false)
    private byte isAuditor;

    @Column(name = "scope_types", columnDefinition = "JSON")
    private String scopeTypes;

    @Column(name = "max_scope_count", nullable = false)
    private int maxScopeCount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusEnum status;
}
