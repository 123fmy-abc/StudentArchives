package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 对话消息 Repository（对应表 ai_messages）
 */
@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    /**
     * 查询某会话下全部消息，按创建时间正序（时间线顺序）
     */
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * 级联软删除某会话下全部消息
     */
    @Modifying
    @Query(value = "UPDATE ai_messages SET deleted_at = :deletedAt WHERE conversation_id = :conversationId AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteByConversationId(@Param("conversationId") Long conversationId, @Param("deletedAt") LocalDateTime deletedAt);
}