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
@Table(name = "archive_competitions")
public class ArchiveCompetition extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "competition_name", nullable = false, length = 255)
    private String competitionName;

    @Column(name = "competition_type", length = 100)
    private String competitionType;

    @Column(name = "award_level", length = 50)
    private String awardLevel;
}
