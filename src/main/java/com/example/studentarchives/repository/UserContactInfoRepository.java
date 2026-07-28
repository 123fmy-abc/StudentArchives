package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户联系信息 Repository
 */
@Repository
public interface UserContactInfoRepository extends JpaRepository<UserContactInfo, Long> {

    Optional<UserContactInfo> findByUserId(Long userId);

    Optional<UserContactInfo> findByEmail(String email);
}
