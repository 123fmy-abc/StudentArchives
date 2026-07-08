package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "award_versions")
public class AwardVersion extends BaseEntity {

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "data_snapshot", columnDefinition = "JSON")
    private String dataSnapshot;

    @Column(name = "status")
    private Integer status;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;
}
