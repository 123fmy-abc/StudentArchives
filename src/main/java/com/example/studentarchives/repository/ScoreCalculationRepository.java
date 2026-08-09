package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.ScoreCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 评分计算批次 Repository（对应表 score_calculations）
 */
@Repository
public interface ScoreCalculationRepository extends JpaRepository<ScoreCalculation, Long> {

    /** 按 ID + 归属用户查询（归属校验） */
    Optional<ScoreCalculation> findByIdAndUserId(Long id, Long userId);
}
