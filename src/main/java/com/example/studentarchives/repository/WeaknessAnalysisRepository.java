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
}
