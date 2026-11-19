package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.ArchiveTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 档案类型配置 Repository
 * <p>
 * 用于字典类型 archive_category 的查询，以及学生端评选说明读取。
 * 评选说明自 V57 起按学校（school_id）独立配置。
 */
@Repository
public interface ArchiveTypeConfigRepository extends JpaRepository<ArchiveTypeConfig, Long> {

    /**
     * 查询指定学校下所有启用的档案类型配置，按 sort 正序
     */
    @Query("SELECT a FROM ArchiveTypeConfig a WHERE a.schoolId = :schoolId AND a.status = 1 ORDER BY a.sort ASC")
    List<ArchiveTypeConfig> findAllActiveBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 查询指定学校下全部档案类型配置（含禁用），按 sort 正序，供管理端配置列表使用
     */
    List<ArchiveTypeConfig> findBySchoolIdOrderBySortAsc(Long schoolId);

    /**
     * 按学校 + 档案类型编码查询配置（含禁用/启用），供管理端保存时复用已有行
     */
    Optional<ArchiveTypeConfig> findBySchoolIdAndArchiveType(Long schoolId, String archiveType);

    /**
     * 按学校 + 档案类型编码查询已启用的配置（学生端评选说明）
     */
    Optional<ArchiveTypeConfig> findBySchoolIdAndArchiveTypeAndStatus(Long schoolId, String archiveType, Integer status);

    /**
     * 取该校排序值最大的配置，用于新增时计算 sort
     */
    Optional<ArchiveTypeConfig> findTopBySchoolIdOrderBySortDesc(Long schoolId);
}
