package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职业规划 Repository
 */
@Repository
public interface CareerPlanRepository extends JpaRepository<CareerPlan, Long> {

    /** 查询学生全部职业规划记录（用于活动列表聚合 + Java 侧筛选） */
    List<CareerPlan> findByUserId(Long userId);

    /** 软删除（通过 native query 绕过 updatable=false 限制） */
    @Modifying
    @Query(value = "UPDATE career_plans SET deleted_at = :deletedAt WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
