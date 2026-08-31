package com.example.studentarchives.repository;

import com.example.studentarchives.entity.log.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核记录 Repository（对应表 audit_logs）
 * <p>
 * 教师首页数据概览按 {@code auditor_id} 统计今日审核数与最近审核动态。
 * 本表为审计日志，无软删除，数据长期保留。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 统计审核人在时间区间内的审核记录数（今日审核数） */
    long countByAuditorIdAndCreatedAtBetween(Long auditorId, LocalDateTime start, LocalDateTime end);

    /** 查询审核人最近审核记录（最近审核动态），按时间倒序 */
    List<AuditLog> findTop10ByAuditorIdOrderByCreatedAtDesc(Long auditorId);
}
