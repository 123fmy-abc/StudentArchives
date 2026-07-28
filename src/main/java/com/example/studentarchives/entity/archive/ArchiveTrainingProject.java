package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "archive_training_projects")
@SQLRestriction("deleted_at IS NULL")
public class ArchiveTrainingProject extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Lob
    @Column(name = "project_content", columnDefinition = "TEXT")
    private String projectContent;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
