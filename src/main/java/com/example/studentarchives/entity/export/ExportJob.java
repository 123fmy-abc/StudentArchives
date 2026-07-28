package com.example.studentarchives.entity.export;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "export_jobs")
public class ExportJob extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "export_type", length = 50, nullable = false)
    private String exportType;

    @Column(name = "scope_type")
    private Integer scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "filter_conditions", columnDefinition = "JSON")
    private String filterConditions;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = 0;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "file_id")
    private Long fileId;

    @Lob
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;
}
