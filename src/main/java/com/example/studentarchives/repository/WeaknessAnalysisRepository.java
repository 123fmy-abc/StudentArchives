package com.example.studentarchives.repository;

import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 短板识别 Repository
 */
@Repository
public interface WeaknessAnalysisRepository extends JpaRepository<WeaknessAnalysis, Long> {

    /**
     * 查询学生全部短板分析，按创建时间倒序（最新在前）
     */
    List<WeaknessAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 按关联来源模型与记录ID查询短板分析（用于 AI 建议的 archive/career_plan 来源映射）
     */
    List<WeaknessAnalysis> findByUserIdAndRelatedTypeAndRelatedId(Long userId, String relatedType, Long relatedId);
}
