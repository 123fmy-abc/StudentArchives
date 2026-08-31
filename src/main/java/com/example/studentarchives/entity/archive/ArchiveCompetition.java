package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "archive_competitions")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class ArchiveCompetition extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "competition_name", nullable = false, length = 255)
    private String competitionName;

    @Column(name = "competition_type", length = 100)
    private String competitionType;

    @Column(name = "award_level", length = 50)
    private String awardLevel;

    @Column(name = "participant_role", length = 50)
    private String participantRole;
}
