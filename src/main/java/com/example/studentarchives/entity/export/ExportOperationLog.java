package com.example.studentarchives.entity.export;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "export_operation_logs")
public class ExportOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "export_type", length = 50, nullable = false)
    private String exportType;

    @Column(name = "action", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer action = 1;

    @Column(name = "scope_type", nullable = false)
    private Integer scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "filter_conditions", columnDefinition = "JSON")
    private String filterConditions;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "is_anonymized", nullable = false, columnDefinition = "TINYINT")
    private Integer isAnonymized;

    @Column(name = "data_version")
    private Integer dataVersion;

    @Lob
    @Column(name = "field_description", columnDefinition = "TEXT")
    private String fieldDescription;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
