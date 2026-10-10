package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerPlanFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职业规划教师反馈 Repository（对应表 career_plan_feedbacks）
 */
@Repository
public interface CareerPlanFeedbackRepository extends JpaRepository<CareerPlanFeedback, Long> {

    /** 按规划 ID 查询教师反馈，按创建时间正序 */
    List<CareerPlanFeedback> findByCareerPlanIdOrderByCreatedAtAsc(Long careerPlanId);

    /** 批量查询多个规划的教师反馈，按创建时间正序（列表取最新一条用） */
    List<CareerPlanFeedback> findByCareerPlanIdInOrderByCreatedAtAsc(List<Long> careerPlanIds);

    /** 软删除（native query 绕过 updatable=false 限制） */
    @Modifying
    @Query(value = "UPDATE career_plan_feedbacks SET deleted_at = :deletedAt WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
