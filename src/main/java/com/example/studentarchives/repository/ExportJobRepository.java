package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 导出任务 Repository（对应表 export_jobs）
 */
@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
}
