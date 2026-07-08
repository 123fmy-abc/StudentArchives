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
@Table(name = "archive_scholarships")
public class ArchiveScholarship extends BaseEntity {

    @Column(name = "archive_id", nullable = false)
    private Long archiveId;

    @Column(name = "scholarship_name", nullable = false, length = 255)
    private String scholarshipName;

    @Column(name = "scholarship_category", length = 50)
    private String scholarshipCategory;

    @Column(name = "award_level", length = 50)
    private String awardLevel;
}
