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
@Table(name = "permissions")
@SQLRestriction("deleted_at IS NULL")
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
