package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiTeacherFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * AI 生成教师反馈 Repository（对应表 ai_teacher_feedbacks）
 */
@Repository
public interface AiTeacherFeedbackRepository extends JpaRepository<AiTeacherFeedback, Long> {

    /**
     * 批量查询生成记录对应的教师反馈
     */
    List<AiTeacherFeedback> findByGenerationLogIdIn(Collection<Long> generationLogIds);
}