package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 职业规划目标 Repository（对应表 career_goals）
 */
@Repository
public interface CareerGoalRepository extends JpaRepository<CareerGoal, Long> {

    /** 按规划 ID 查询目标，按 sort 正序 */
    List<CareerGoal> findByCareerPlanIdOrderBySortAsc(Long careerPlanId);

    /** 按 ID + 规划 ID 查询目标（归属校验） */
    Optional<CareerGoal> findByIdAndCareerPlanId(Long id, Long careerPlanId);

    /** 取规划下最大 sort 的目标 */
    Optional<CareerGoal> findTopByCareerPlanIdOrderBySortDesc(Long careerPlanId);

    /**
     * 软删除目标（通过 native query 绕过 updatable=false 限制）。
     * 仅置 deleted_at，由生成列 is_deleted_null 配合 @SQLRestriction 自动过滤。
     *
     * @param id        目标 ID
     * @param deletedAt 软删除时间
     * @return 受影响行数（0 表示已不存在）
     */
    @Modifying
    @Query(value = "UPDATE career_goals SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
