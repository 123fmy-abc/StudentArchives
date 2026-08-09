package com.example.studentarchives.repository;

import com.example.studentarchives.entity.message.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户消息通知设置 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 查询用户某分类的通知设置（条件唯一索引 (user_id, category)）
     */
    Optional<NotificationSetting> findByUserIdAndCategory(Long userId, String category);

    /**
     * 查询用户全部通知设置
     */
    List<NotificationSetting> findByUserId(Long userId);
}
