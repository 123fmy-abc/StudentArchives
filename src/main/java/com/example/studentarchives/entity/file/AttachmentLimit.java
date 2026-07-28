package com.example.studentarchives.entity.file;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "attachment_limits")
public class AttachmentLimit extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "allowed_extensions", columnDefinition = "JSON")
    private String allowedExtensions;

    @Column(name = "max_file_size")
    private Long maxFileSize;

    @Column(name = "max_files", nullable = false)
    private Integer maxFiles;

    @Column(name = "min_files", nullable = false)
    private Integer minFiles;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
