package com.example.studentarchives.repository.log;

import com.example.studentarchives.entity.log.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 登录日志仓库
 */
@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
