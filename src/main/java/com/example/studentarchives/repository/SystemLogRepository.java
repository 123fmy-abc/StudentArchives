package com.example.studentarchives.repository;

import com.example.studentarchives.entity.log.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 系统操作日志仓储（对应表 system_logs）
 * <p>
 * 支持 JPA Specification 动态筛选，供管理端 GET /admin/logs/system 分页查询。
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {
}