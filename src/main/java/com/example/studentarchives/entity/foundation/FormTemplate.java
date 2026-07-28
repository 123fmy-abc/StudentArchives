package com.example.studentarchives.entity.foundation;

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
@Table(name = "form_templates")
@SQLRestriction("deleted_at IS NULL")
public class FormTemplate extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "category", nullable = false, length = 50)
    private String category = "archive";

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "fields", columnDefinition = "JSON")
    private String fields;

    @Column(name = "layout_config", columnDefinition = "JSON")
    private String layoutConfig;

    @Column(name = "applicable_roles", columnDefinition = "JSON")
    private String applicableRoles;

    @Column(name = "is_default")
    private Byte isDefault;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
