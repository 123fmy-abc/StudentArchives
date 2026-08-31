package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "archive_innovations")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ArchiveInnovation extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "industry_type", length = 100)
    private String industryType;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(name = "participant_role", length = 50)
    private String participantRole;

    @Column(name = "registered_at")
    private LocalDate registeredAt;
}
