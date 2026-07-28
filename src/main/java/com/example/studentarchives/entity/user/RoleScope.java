package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "role_scopes")
public class RoleScope extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "scope_type", nullable = false)
    private Integer scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "is_primary")
    private Integer isPrimary;

    @Column(name = "appoint_by")
    private Long appointBy;

    @Column(name = "appoint_reason", length = 255)
    private String appointReason;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
