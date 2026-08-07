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

    /**
     * 查询学生全部画像得分，按学期倒序（用于成长摘要取最近一次评估）
     */
    List<PortraitEvaluationScore> findByUserIdOrderBySemesterIdDesc(Long userId);
}
