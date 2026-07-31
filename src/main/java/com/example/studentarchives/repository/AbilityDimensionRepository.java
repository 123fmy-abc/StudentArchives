package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.AbilityDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 能力维度 Repository
 */
@Repository
public interface AbilityDimensionRepository extends JpaRepository<AbilityDimension, Long> {

    /**
     * 查询所有启用的能力维度，按 sort 正序
     */
    @Query("SELECT a FROM AbilityDimension a WHERE a.status = 1 ORDER BY a.sort ASC")
    List<AbilityDimension> findAllActive();
}
