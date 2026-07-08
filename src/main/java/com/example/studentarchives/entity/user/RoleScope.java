package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "role_scopes")
public class RoleScope extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "scope_type", nullable = false)
    private byte scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "is_primary", nullable = false)
    private byte isPrimary;

    @Column(name = "appoint_by", nullable = false)
    private Long appointBy;

    @Column(name = "appoint_reason", length = 255)
    private String appointReason;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "status", nullable = false)
    private byte status;
}
