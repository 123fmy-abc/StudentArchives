package com.example.studentarchives.repository;

import com.example.studentarchives.entity.log.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 审核记录 Repository（对应表 audit_logs）
 * <p>
 * 供教师端「待审核任务模块」（《教师端接口文档》四）写入通过/退回/撤销记录。
 * 注意：audit_logs 无软删除过滤（实体继承 BaseEntityNoUpdate，不映射 deleted_at）。
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
}