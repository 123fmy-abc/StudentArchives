package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * AI 对话会话 Repository（对应表 ai_conversations）
 */
@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    /**
     * 查询指定用户正常状态下的会话，按最后对话时间倒序（最新在前）
     */
    Page<AiConversation> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Integer status, Pageable pageable);

    /**
     * 软删除会话
     */
    @Modifying
    @Query(value = "UPDATE ai_conversations SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}