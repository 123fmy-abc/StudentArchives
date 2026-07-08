package com.example.studentarchives.entity.archive;
import com.example.studentarchives.enums.StatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

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
@Table(name = "archive_type_config")
public class ArchiveTypeConfig extends BaseEntity {

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Lob
    @Column(name = "evaluate_desc", columnDefinition = "TEXT")
    private String evaluateDesc;

    @Lob
    @Column(name = "apply_desc", columnDefinition = "TEXT")
    private String applyDesc;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "sort", nullable = false)
    private int sort;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private StatusEnum status;
}
