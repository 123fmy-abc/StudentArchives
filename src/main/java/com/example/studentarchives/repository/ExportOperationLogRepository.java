package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 导出操作日志 Repository（对应表 export_operation_logs）
 */
@Repository
public interface ExportOperationLogRepository extends JpaRepository<ExportOperationLog, Long>,
        JpaSpecificationExecutor<ExportOperationLog> {
}
