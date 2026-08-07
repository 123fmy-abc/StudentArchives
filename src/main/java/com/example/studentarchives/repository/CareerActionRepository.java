package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 职业规划行动 Repository（对应表 career_actions）
 */
@Repository
public interface CareerActionRepository extends JpaRepository<CareerAction, Long> {

    /** 按目标 ID 查询行动，按 sort 正序 */
    List<CareerAction> findByGoalIdOrderBySortAsc(Long goalId);

    /** 按 ID + 目标 ID 查询行动（归属校验） */
    Optional<CareerAction> findByIdAndGoalId(Long id, Long goalId);

    /** 取目标下最大 sort 的行动 */
    Optional<CareerAction> findTopByGoalIdOrderBySortDesc(Long goalId);

    /**
     * 软删除行动（通过 native query 绕过 updatable=false 限制）。
     * 仅置 deleted_at，由生成列 is_deleted_null 配合 @SQLRestriction 自动过滤。
     *
     * @param id        行动 ID
     * @param deletedAt 软删除时间
     * @return 受影响行数（0 表示已不存在）
     */
    @Modifying
    @Query(value = "UPDATE career_actions SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
