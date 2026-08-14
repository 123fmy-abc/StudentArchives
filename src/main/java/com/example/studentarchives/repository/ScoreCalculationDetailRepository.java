package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.ScoreCalculationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 评分计算明细 Repository（对应表 score_calculation_details）
 */
@Repository
public interface ScoreCalculationDetailRepository extends JpaRepository<ScoreCalculationDetail, Long> {

    /** 按计算批次 ID 查询明细 */
    List<ScoreCalculationDetail> findByCalculationId(Long calculationId);

    /** 按计算批次 ID 列表批量查询明细 */
    List<ScoreCalculationDetail> findByCalculationIdIn(Collection<Long> calculationIds);

    /**
     * 软删除指定计算批次集合下的明细（评分重算前清理旧明细）。
     * 注：score_calculation_details 实体基于 BaseEntityNoUpdate（无 deleted_at 字段映射），故使用 native 更新。
     */
    @Modifying
    @Query(value = "UPDATE score_calculation_details SET deleted_at = :deletedAt "
            + "WHERE calculation_id IN (:calculationIds) AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteByCalculationIds(@Param("calculationIds") Collection<Long> calculationIds,
                                   @Param("deletedAt") LocalDateTime deletedAt);
}
