package com.example.studentarchives.entity.career;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "career_plans")
public class CareerPlan extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "copy_from_id")
    private Long copyFromId;

    @Column(name = "source", nullable = false)
    private Integer source = 1;

    @Column(name = "ai_suggestion_id")
    private Long aiSuggestionId;

    @Column(name = "require_confirm", nullable = false)
    private Integer requireConfirm = 1;

    @Column(name = "progress_rate", nullable = false)
    private Integer progressRate;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /** 提交流程公共字段（提交/审核/退回/通过/撤销时间戳等） */
    @Embedded
    private ArchiveAuditInfo auditInfo = new ArchiveAuditInfo();

    /** 生成的主文件ID（如导出的规划PDF）；上传的佐证材料通过 file_uploads.biz_type='career_plan' 关联多个文件 */
    @Column(name = "file_id")
    private Long exportFileId;
}
