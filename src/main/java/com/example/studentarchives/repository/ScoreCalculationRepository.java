package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.ScoreCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 评分计算批次 Repository（对应表 score_calculations）
 */
@Repository
public interface ScoreCalculationRepository extends JpaRepository<ScoreCalculation, Long> {

    /** 按 ID + 归属用户查询（归属校验） */
    Optional<ScoreCalculation> findByIdAndUserId(Long id, Long userId);

    /** 查询学生某学期的全部评分计算批次（评分重算时替换旧批次） */
    List<ScoreCalculation> findByUserIdAndSemesterId(Long userId, Long semesterId);

    /**
     * 软删除学生某学期的旧评分计算批次（评分重算前清理，避免与明细/画像分数重复）。
     * 注：score_calculations 实体基于 BaseEntityNoUpdate（无 deleted_at 字段映射），故使用 native 更新。
     */
    @Modifying
    @Query(value = "UPDATE score_calculations SET deleted_at = :deletedAt "
            + "WHERE user_id = :userId AND semester_id = :semesterId AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteByUserIdAndSemesterId(@Param("userId") Long userId,
                                        @Param("semesterId") Long semesterId,
                                        @Param("deletedAt") LocalDateTime deletedAt);
}
