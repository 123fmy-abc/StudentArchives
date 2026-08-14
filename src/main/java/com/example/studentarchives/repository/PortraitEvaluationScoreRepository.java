package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 画像评估得分 Repository
 */
@Repository
public interface PortraitEvaluationScoreRepository extends JpaRepository<PortraitEvaluationScore, Long> {

    /**
     * 查询学生某学期的各维度画像得分
     */
    List<PortraitEvaluationScore> findByUserIdAndSemesterId(Long userId, Long semesterId);

    /**
     * 查询学生全部画像得分，按学期倒序（用于成长摘要取最近一次评估）
     */
    List<PortraitEvaluationScore> findByUserIdOrderBySemesterIdDesc(Long userId);

    /**
     * 软删除学生某学期的画像得分（评分重算前清理旧数据。
     * 表上有条件唯一键 uk_pes_user_semester_dimension，必须先软删旧行再插入新行）。
     */
    @Modifying
    @Query(value = "UPDATE portrait_evaluation_scores SET deleted_at = :deletedAt "
            + "WHERE user_id = :userId AND semester_id = :semesterId AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteByUserIdAndSemesterId(@Param("userId") Long userId,
                                        @Param("semesterId") Long semesterId,
                                        @Param("deletedAt") LocalDateTime deletedAt);
}
