package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.EvaluationIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评价指标 Repository
 */
@Repository
public interface EvaluationIndicatorRepository extends JpaRepository<EvaluationIndicator, Long> {

    /**
     * 查询指定版本下所有启用的指标，按 sort 正序
     */
    @Query("SELECT e FROM EvaluationIndicator e WHERE e.version = :version AND e.status = 1 ORDER BY e.sort ASC")
    List<EvaluationIndicator> findActiveByVersion(@Param("version") Integer version);
}
