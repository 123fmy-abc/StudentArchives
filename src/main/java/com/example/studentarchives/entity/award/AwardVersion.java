package com.example.studentarchives.entity.award;
import com.example.studentarchives.enums.ApplyStatusEnum;
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
@Table(name = "award_versions")
public class AwardVersion extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "data_snapshot", nullable = false, columnDefinition = "JSON")
    private String dataSnapshot;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private ApplyStatusEnum status;

    @Lob
    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;
}
