package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.AwardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 个人奖项汇总 Repository
 */
@Repository
public interface AwardSummaryRepository extends JpaRepository<AwardSummary, Long> {

    /**
     * 查询学生全部奖项汇总
     */
    List<AwardSummary> findByUserId(Long userId);
}
