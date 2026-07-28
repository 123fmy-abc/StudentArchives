package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_teacher_feedbacks")
public class AiTeacherFeedback extends BaseEntity {

    @Column(name = "generation_log_id", nullable = false)
    private Long generationLogId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "action")
    private Integer action;

    @Lob
    @Column(name = "modified_content", columnDefinition = "TEXT")
    private String modifiedContent;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;
}
