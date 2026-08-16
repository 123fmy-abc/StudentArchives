package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 导出操作日志 Repository（对应表 export_operation_logs）
 */
@Repository
public interface ExportOperationLogRepository extends JpaRepository<ExportOperationLog, Long>,
        JpaSpecificationExecutor<ExportOperationLog> {

    /**
     * 查询某个文件最近一次指定操作的日志（下载审计以创建记录为模板）
     *
     * @param fileId 文件 ID
     * @param action 操作类型（1=创建，2=下载）
     */
    Optional<ExportOperationLog> findTopByFileIdAndActionOrderByCreatedAtDesc(Long fileId, Integer action);
}
