package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "archive_researches")
public class ArchiveResearch extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_level", length = 50)
    private String projectLevel;

    @Column(name = "project_type", length = 100)
    private String projectType;

    @Column(name = "team_role", length = 50)
    private String teamRole;

    @Column(name = "project_time", length = 20)
    private String projectTime;
}
