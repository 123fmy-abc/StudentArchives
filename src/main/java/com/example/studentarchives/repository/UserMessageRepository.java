package com.example.studentarchives.repository;

import com.example.studentarchives.entity.message.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息 Repository
 */
@Repository
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    /**
     * 统计未读消息数（未读且未归档）
     */
    long countByUserIdAndIsReadAndIsArchived(Long userId, Integer isRead, Integer isArchived);

    /**
     * 统计指定分类下未读消息数（未读且未归档）
     */
    long countByUserIdAndIsReadAndIsArchivedAndCategory(Long userId, Integer isRead, Integer isArchived, String category);

    /**
     * 分页查询消息列表（文档 5.1）
     * <p>
     * category/isRead/keyword 可空，isArchived 必传（默认 0 未归档）。
     * keyword 匹配 title 或 content；按 createdAt 倒序。
     */
    @Query("SELECT m FROM UserMessage m WHERE m.userId = :userId "
            + "AND (:category IS NULL OR m.category = :category) "
            + "AND (:isRead IS NULL OR m.isRead = :isRead) "
            + "AND m.isArchived = :isArchived "
            + "AND (:keyword IS NULL OR :keyword = '' "
            + "     OR m.title LIKE CONCAT('%', :keyword, '%') "
            + "     OR m.content LIKE CONCAT('%', :keyword, '%'))")
    Page<UserMessage> search(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("isRead") Integer isRead,
            @Param("isArchived") Integer isArchived,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 批量标记已读（文档 5.3）
     * <p>
     * 将当前用户未读消息标记为已读；category 可空（空则标记该用户全部未读）。
     * 返回受影响行数。
     */
    @Modifying
    @Query("UPDATE UserMessage m SET m.isRead = 1, m.readAt = :now "
            + "WHERE m.userId = :userId AND m.isRead = 0 "
            + "AND (:category IS NULL OR m.category = :category)")
    int markAllRead(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("now") LocalDateTime now);

    /**
     * 按 ID 列表批量标记已读（文档 5.8）
     * <p>
     * 仅标记当前用户未读消息；已读/非本人/已软删消息静默跳过。
     * 返回受影响行数。JPQL 批量更新不继承 @SQLRestriction，需显式附加 deletedAt IS NULL。
     */
    @Modifying
    @Query("UPDATE UserMessage m SET m.isRead = 1, m.readAt = :now "
            + "WHERE m.userId = :userId AND m.id IN :ids AND m.isRead = 0 "
            + "AND m.deletedAt IS NULL")
    int markReadByIds(
            @Param("userId") Long userId,
            @Param("ids") List<Long> ids,
            @Param("now") LocalDateTime now);

    /**
     * 软删除单条消息（文档 5.9）
     * <p>
     * deleted_at 列 insertable/updatable=false，须用 native query 绕过（与
     * UserInterestRepository.softDeleteById 同款模式）。返回受影响行数。
     */
    @Modifying
    @Query(value = "UPDATE user_messages SET deleted_at = :deletedAt "
            + "WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 按 ID 列表批量软删除（文档 5.10）
     * <p>
     * 仅软删当前用户消息；非本人/已软删消息静默跳过。返回受影响行数。
     */
    @Modifying
    @Query(value = "UPDATE user_messages SET deleted_at = :deletedAt "
            + "WHERE user_id = :userId AND id IN (:ids) AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteByIds(
            @Param("userId") Long userId,
            @Param("ids") List<Long> ids,
            @Param("deletedAt") LocalDateTime deletedAt);
}
