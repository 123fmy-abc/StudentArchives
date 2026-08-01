package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.PortraitEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
