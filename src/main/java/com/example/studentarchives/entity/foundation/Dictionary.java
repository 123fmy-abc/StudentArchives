package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dictionaries")
public class Dictionary extends BaseEntity {

    @Column(name = "dict_type", nullable = false, length = 50)
    private String dictType;

    @Column(name = "dict_code", nullable = false, length = 50)
    private String dictCode;

    @Column(name = "dict_name", nullable = false, length = 100)
    private String dictName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
