package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
     * 批量查询多个学生的全部档案记录（管理端一键导出档案列表用，避免逐学生查询）
     */
    List<Archive> findByUserIdIn(Collection<Long> userIds);

    /**
     * 查询学生最近提交的档案记录（按 submitted_at 倒序），用于首页最近动态
     */
    List<Archive> findTop5ByUserIdAndAuditInfo_SubmittedAtIsNotNullOrderByAuditInfo_SubmittedAtDesc(Long userId);

    /**
     * 按档案类型筛选
     */
    List<Archive> findByUserIdAndArchiveType(Long userId, String archiveType);

    /**
     * 按学生 + 状态筛选（评分重算仅统计已通过 status=2 的档案）
     */
    List<Archive> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 软删除（通过 native query 绕过 updatable=false 限制）
     */
    @Modifying
    @Query(value = "UPDATE archives SET deleted_at = :deletedAt WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") java.time.LocalDateTime deletedAt);

    /**
     * 统计某学校某学期有效档案总数
     */
    @Query("SELECT COUNT(a) FROM Archive a WHERE a.schoolId = :schoolId AND a.semesterId = :semesterId")
    Long countBySchoolIdAndSemesterId(@Param("schoolId") Long schoolId, @Param("semesterId") Long semesterId);

    /**
     * 统计某学校某学期有档案的学生去重数
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM Archive a WHERE a.schoolId = :schoolId AND a.semesterId = :semesterId")
    Long countDistinctUserIdBySchoolIdAndSemesterId(@Param("schoolId") Long schoolId, @Param("semesterId") Long semesterId);

    /**
     * 按档案类型分组统计某学校某学期数量（返回 [archive_type, count]）
     */
    @Query(value = "SELECT archive_type, COUNT(*) " +
            "FROM archives WHERE school_id = :schoolId AND semester_id = :semesterId AND deleted_at IS NULL " +
            "GROUP BY archive_type", nativeQuery = true)
    List<Object[]> countGroupByArchiveType(@Param("schoolId") Long schoolId, @Param("semesterId") Long semesterId);
}
