package com.example.studentarchives.entity.foundation;
import com.example.studentarchives.enums.StatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "fields", nullable = false, columnDefinition = "JSON")
    private String fields;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusEnum status;
}
