package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.ScoreCalculationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评分计算明细 Repository（对应表 score_calculation_details）
 */
@Repository
public interface ScoreCalculationDetailRepository extends JpaRepository<ScoreCalculationDetail, Long> {

    /** 按计算批次 ID 查询明细 */
    List<ScoreCalculationDetail> findByCalculationId(Long calculationId);
}
