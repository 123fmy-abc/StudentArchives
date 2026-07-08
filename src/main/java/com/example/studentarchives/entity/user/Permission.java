package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "type")
    private Integer type;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Permission parent;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "status")
    private Integer status;
}
