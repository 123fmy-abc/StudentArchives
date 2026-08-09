package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 职业规划里程碑 Repository（对应表 career_milestones）
 */
@Repository
public interface CareerMilestoneRepository extends JpaRepository<CareerMilestone, Long> {

    /** 按行动 ID 查询里程碑，按 sort 正序 */
    List<CareerMilestone> findByActionIdOrderBySortAsc(Long actionId);

    /** 取行动下最大 sort 的里程碑 */
    Optional<CareerMilestone> findTopByActionIdOrderBySortDesc(Long actionId);

    /**
     * 软删除里程碑（通过 native query 绕过 updatable=false 限制）。
     * 仅置 deleted_at，由生成列 is_deleted_null 配合 @SQLRestriction 自动过滤。
     *
     * @param id        里程碑 ID
     * @param deletedAt 软删除时间
     * @return 受影响行数（0 表示已不存在）
     */
    @Modifying
    @Query(value = "UPDATE career_milestones SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
