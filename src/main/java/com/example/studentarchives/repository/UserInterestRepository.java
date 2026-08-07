package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户兴趣 Repository
 */
@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    /**
     * 查询学生全部兴趣标签，按 sort 正序
     */
    List<UserInterest> findByUserIdOrderBySortAsc(Long userId);

    /**
     * 软删除兴趣标签（通过 native query 绕过 updatable=false 限制）。
     * 仅置 deleted_at，由生成列 is_deleted_null 配合条件唯一索引 uk_ui_user_tag，
     * 保证删除后可重新添加同名标签。
     *
     * @param id         兴趣标签 ID
     * @param deletedAt  软删除时间
     * @return 受影响行数（0 表示已不存在）
     */
    @Modifying
    @Query(value = "UPDATE user_interests SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
