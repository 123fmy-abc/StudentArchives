package com.example.studentarchives.entity.ai;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * AI 消息反馈（学生端）
 * <p>
 * 与教师端 {@code ai_teacher_feedbacks} 分离，记录学生对 AI 回答的有用/无用反馈。
 */
@Getter
@Setter
@Entity
@Table(name = "ai_message_feedbacks")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class AiMessageFeedback extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    /** 反馈枚举：useful=有用 useless=无用 */
    @Column(name = "feedback", nullable = false, length = 20)
    private String feedback;
}