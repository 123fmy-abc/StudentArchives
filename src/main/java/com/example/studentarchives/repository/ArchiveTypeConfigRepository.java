package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.ArchiveTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 档案类型配置 Repository
 * <p>
 * 用于字典类型 archive_category 的查询。
 */
@Repository
public interface ArchiveTypeConfigRepository extends JpaRepository<ArchiveTypeConfig, Long> {

    /**
     * 查询指定学校下所有启用的档案类型配置，按 sort 正序
     */
    @Query("SELECT a FROM ArchiveTypeConfig a WHERE a.status = 1 ORDER BY a.sort ASC")
    List<ArchiveTypeConfig> findAllActive();
}
