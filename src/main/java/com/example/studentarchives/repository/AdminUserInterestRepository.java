package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
