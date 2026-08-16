package com.example.studentarchives.repository;

import com.example.studentarchives.entity.log.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 登录日志 Repository
 */
@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {

    Optional<LoginLog> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
