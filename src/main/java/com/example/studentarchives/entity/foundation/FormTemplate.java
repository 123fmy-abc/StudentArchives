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
@Table(name = "form_templates")
public class FormTemplate extends BaseEntity {

    private String name;

    private String code;

    @Column(columnDefinition = "JSON")
    private String fields;

    private Integer status;
}
