package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "dictionaries")
public class Dictionary extends BaseEntity {

    @Column(name = "dict_type")
    private String dictType;

    @Column(name = "dict_code")
    private String dictCode;

    @Column(name = "dict_name")
    private String dictName;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Dictionary parent;

    @OneToMany(mappedBy = "parent")
    private List<Dictionary> children;

    private Integer sort;

    private Integer status;
}
