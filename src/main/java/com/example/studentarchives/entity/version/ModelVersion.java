package com.example.studentarchives.entity.version;

import com.example.studentarchives.entity.BaseEntityNoUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "model_versions")
public class ModelVersion extends BaseEntityNoUpdate {

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "data_snapshot", columnDefinition = "JSON")
    private String dataSnapshot;

    @Column(name = "status")
    private Integer status;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "change_summary", length = 255)
    private String changeSummary;

    @Column(name = "created_by")
    private Long createdBy;
}
