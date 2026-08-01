package com.example.studentarchives.repository;

import com.example.studentarchives.entity.message.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 站内消息 Repository
 */
@Repository
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    /**
     * 统计未读消息数（未读且未归档）
     */
    long countByUserIdAndIsReadAndIsArchived(Long userId, Integer isRead, Integer isArchived);
}
