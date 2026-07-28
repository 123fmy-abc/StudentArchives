package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "archive_certificates")
@SQLRestriction("deleted_at IS NULL")
public class ArchiveCertificate extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "certificate_type", nullable = false, length = 100)
    private String certificateType;

    @Column(name = "certificate_name", nullable = false, length = 255)
    private String certificateName;

    @Column(name = "certificate_no", length = 100)
    private String certificateNo;

    @Column(name = "issuing_unit", length = 255)
    private String issuingUnit;

    @Column(name = "valid_until")
    private LocalDate validUntil;
}
