package com.example.studentarchives.entity.award;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "award_applications")
public class AwardApplication extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "award_type", nullable = false, length = 50)
    private String awardType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "certificate_no", length = 100)
    private String certificateNo;

    @Column(name = "issuing_unit", length = 255)
    private String issuingUnit;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "participant_role", length = 50)
    private String participantRole;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /** 提交流程公共字段（提交/审核/退回/通过/撤销时间戳等） */
    @Embedded
    private ArchiveAuditInfo auditInfo;
}
