package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserInterest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 用户兴趣补充 Repository（对应表 user_interests）
 * <p>
 * 管理端统计模块批量聚合 topInterests 使用。
 */
@Repository
public interface AdminUserInterestRepository extends JpaRepository<UserInterest, Long> {

    /** 批量查询多个学生的兴趣标签 */
    List<UserInterest> findByUserIdIn(Collection<Long> userIds);

    /**
     * 按学校聚合热门兴趣标签 TOP N（返回 [tagName, count]）
     */
    @Query("SELECT ui.tagName, COUNT(ui.id) FROM UserInterest ui " +
            "WHERE ui.userId IN (SELECT u.id FROM User u WHERE u.schoolId = :schoolId) " +
            "GROUP BY ui.tagName ORDER BY COUNT(ui.id) DESC")
    List<Object[]> countGroupByTagNameTopN(@Param("schoolId") Long schoolId, Pageable pageable);
}
