package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 档案 Repository
 */
@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    /**
     * 查询学生全部档案记录（用于申报统计与快捷入口 recent 判断）
     */
    List<Archive> findByUserId(Long userId);

    /**
     * 查询学生最近提交的档案记录（按 submitted_at 倒序），用于首页最近动态
     */
    List<Archive> findTop5ByUserIdAndAuditInfo_SubmittedAtIsNotNullOrderByAuditInfo_SubmittedAtDesc(Long userId);

    /**
     * 按档案类型筛选
     */
    List<Archive> findByUserIdAndArchiveType(Long userId, String archiveType);

    /**
     * 软删除（通过 native query 绕过 updatable=false 限制）
     */
    @Modifying
    @Query(value = "UPDATE archives SET deleted_at = :deletedAt WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") java.time.LocalDateTime deletedAt);
}
