package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiMessageFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AI 消息反馈（学生端）Repository
 */
@Repository
public interface AiMessageFeedbackRepository extends JpaRepository<AiMessageFeedback, Long> {

    Optional<AiMessageFeedback> findByMessageIdAndUserId(Long messageId, Long userId);
}