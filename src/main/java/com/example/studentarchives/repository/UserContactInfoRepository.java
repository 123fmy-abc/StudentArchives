package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户联系信息 Repository
 */
@Repository
public interface UserContactInfoRepository extends JpaRepository<UserContactInfo, Long> {

    Optional<UserContactInfo> findByUserId(Long userId);

    Optional<UserContactInfo> findByEmail(String email);

    List<UserContactInfo> findByUserIdIn(Collection<Long> userIds);

    /**
     * 关键词模糊匹配手机号/邮箱（管理端用户列表搜索）
     */
    List<UserContactInfo> findByPhoneContainingOrEmailContaining(String phone, String email);
}
