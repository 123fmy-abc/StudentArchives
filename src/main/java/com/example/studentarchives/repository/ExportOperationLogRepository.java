package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 导出操作日志 Repository（对应表 export_operation_logs）
 */
@Repository
public interface ExportOperationLogRepository extends JpaRepository<ExportOperationLog, Long> {

    /**
     * 查询某导出文件最近一条指定 action 的操作日志（下载审计取 action=1 创建记录为模板）
     */
    Optional<ExportOperationLog> findTopByFileIdAndActionOrderByCreatedAtDesc(Long fileId, Integer action);
}
