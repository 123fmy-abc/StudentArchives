package com.example.studentarchives.entity.grade;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "grade_import_configs")
public class GradeImportConfig extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "allowed_extensions", columnDefinition = "JSON")
    private String allowedExtensions;

    @Column(name = "max_file_size")
    private Long maxFileSize;

    @Column(name = "template_columns", columnDefinition = "JSON")
    private String templateColumns;

    @Column(name = "has_header_row", nullable = false)
    private Integer hasHeaderRow;

    @Column(name = "batch_size", nullable = false)
    private int batchSize;

    @Column(name = "allow_overwrite", nullable = false)
    private Integer allowOverwrite;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;
}
