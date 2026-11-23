package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiFaqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 常见问题 Repository
 */
@Repository
public interface AiFaqQuestionRepository extends JpaRepository<AiFaqQuestion, Long> {

    /**
     * 查询所有启用的常见问题，按 sort 正序（学生端 /ai/faqs）
     */
    @Query("SELECT a FROM AiFaqQuestion a WHERE a.status = 1 ORDER BY a.sort ASC")
    List<AiFaqQuestion> findAllActive();

    /**
     * 查询所有常见问题（含禁用），按 sort 正序（管理端）
     */
    List<AiFaqQuestion> findAllByOrderBySortAsc();

    /**
     * 软删除常见问题（置 deleted_at）
     */
    @Modifying
    @Query(value = "UPDATE ai_faq_questions SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
