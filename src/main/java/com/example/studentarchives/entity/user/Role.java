package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "level")
    private Integer level;

    @Column(name = "is_system")
    private Integer isSystem;

    @Column(name = "is_auditor")
    private Integer isAuditor;

    @Column(name = "scope_types", columnDefinition = "JSON")
    private String scopeTypes;

    @Column(name = "max_scope_count")
    private Integer maxScopeCount;

    @Column(name = "status")
    private Integer status;
}
