package com.example.studentarchives.repository;

import com.example.studentarchives.entity.log.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审核记录 Repository（对应表 audit_logs）
 * <p>
 * 供教师端「待审核任务模块」（《教师端接口文档》四）写入通过/退回/撤销记录、
 * 「审核历史模块」5.1 查询，以及教师首页数据概览按 {@code auditor_id} 统计今日审核数与最近审核动态。
 * 注意：audit_logs 无软删除过滤（实体继承 BaseEntityNoUpdate，不映射 deleted_at），数据长期保留。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 查询某模型（approvable）的全部审核记录，按 ID 升序（即时间升序） */
    List<AuditLog> findByAuditableTypeAndAuditableIdOrderByIdAsc(String auditableType, Long auditableId);

    /** 查询某模型最近一次指定动作的审核记录（撤销时用于关联被撤销的通过记录） */
    Optional<AuditLog> findTopByAuditableTypeAndAuditableIdAndActionOrderByIdDesc(
            String auditableType, Long auditableId, Integer action);

    /** 查询某审核人（教师）的全部审核记录，按 ID 倒序（时间倒序），供「审核历史」模块 5.1 使用 */
    List<AuditLog> findByAuditorIdOrderByIdDesc(Long auditorId);

    /** 统计审核人在时间区间内的审核记录数（今日审核数） */
    long countByAuditorIdAndCreatedAtBetween(Long auditorId, LocalDateTime start, LocalDateTime end);

    /** 查询审核人最近审核记录（最近审核动态），按时间倒序 */
    List<AuditLog> findTop10ByAuditorIdOrderByCreatedAtDesc(Long auditorId);
}