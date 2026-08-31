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
@Table(name = "archive_researches")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ArchiveResearch extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_level", nullable = false, length = 50)
    private String projectLevel;

    @Column(name = "project_type", nullable = false, length = 100)
    private String projectType;

    @Column(name = "participant_role", length = 50)
    private String participantRole;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
