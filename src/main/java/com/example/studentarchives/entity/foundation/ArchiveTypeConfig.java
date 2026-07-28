package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "archive_type_configs")
public class ArchiveTypeConfig extends BaseEntity {

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Lob
    @Column(name = "evaluate_desc", columnDefinition = "TEXT")
    private String evaluateDesc;

    @Column(name = "evaluate_requirements", columnDefinition = "JSON")
    private String evaluateRequirements;

    @Column(name = "evaluate_notes", columnDefinition = "JSON")
    private String evaluateNotes;

    @Lob
    @Column(name = "apply_desc", columnDefinition = "TEXT")
    private String applyDesc;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
