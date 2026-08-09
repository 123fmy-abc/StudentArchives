package com.example.studentarchives.entity.archive;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "archives")
public class Archive extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "archive_type", nullable = false, length = 50)
    private String archiveType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "obtained_at")
    private LocalDate obtainedAt;

    @Column(name = "duplicate_check_status", nullable = false)
    private Integer duplicateCheckStatus;

    @Column(name = "duplicate_info", columnDefinition = "JSON")
    private String duplicateInfo;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /** 提交流程公共字段（提交/审核/退回/通过/撤销时间戳等） */
    @Embedded
    private ArchiveAuditInfo auditInfo = new ArchiveAuditInfo();

    /** 更正原因（仅 archives 表有此列，不在 ArchiveAuditInfo 中） */
    @Lob
    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;
}
