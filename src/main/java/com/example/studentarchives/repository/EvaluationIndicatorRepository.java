package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 评价指标 Repository
 */
@Repository
public interface EvaluationIndicatorRepository extends JpaRepository<EvaluationIndicator, Long> {

    /**
     * 查询指定版本下所有启用的指标，按 sort 正序
     */
    @Query("SELECT e FROM EvaluationIndicator e WHERE e.version = :version AND e.status = 1 ORDER BY e.sort ASC")
    List<EvaluationIndicator> findActiveByVersion(@Param("version") Integer version);

    /**
     * 批量查询指标（用于画像分数计算明细的名称映射）
     */
    List<EvaluationIndicator> findByIdIn(Collection<Long> ids);

    // ==================== 管理端指标配置 ====================

    /**
     * 查询学校下全部指标（未删除），按 sort 正序
     */
    List<EvaluationIndicator> findBySchoolIdOrderBySortAsc(Long schoolId);

    /**
     * 查询学校下指定状态的全部指标（未删除），按 sort 正序
     */
    List<EvaluationIndicator> findBySchoolIdAndStatusOrderBySortAsc(Long schoolId, Integer status);

    /**
     * 查询学校下全部一级指标（parent_id IS NULL，未删除），按 sort 正序
     */
    List<EvaluationIndicator> findBySchoolIdAndParentIdIsNullOrderBySortAsc(Long schoolId);

    /**
     * 查询某指标下的直属子指标（未删除）
     */
    List<EvaluationIndicator> findByParentIdOrderBySortAsc(Long parentId);

    /**
     * 按编码查询学校下指标（用于唯一性校验，未删除）
     */
    Optional<EvaluationIndicator> findBySchoolIdAndIndicatorCode(Long schoolId, String indicatorCode);

    /**
     * 发布指标规则版本时，将全校指标快照版本号统一推进到新版本
     */
    @Modifying
    @Query("UPDATE EvaluationIndicator e SET e.version = :version, e.updatedAt = :now WHERE e.schoolId = :schoolId")
    int restampVersion(@Param("schoolId") Long schoolId, @Param("version") Integer version, @Param("now") LocalDateTime now);

    /**
     * 软删除指标（置 deleted_at）
     */
    @Modifying
    @Query("UPDATE EvaluationIndicator e SET e.deletedAt = :deletedAt, e.updatedAt = :deletedAt WHERE e.id = :id")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
