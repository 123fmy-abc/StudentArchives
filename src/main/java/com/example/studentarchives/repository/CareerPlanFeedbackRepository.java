package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerPlanFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 职业规划教师反馈 Repository（对应表 career_plan_feedbacks）
 */
@Repository
public interface CareerPlanFeedbackRepository extends JpaRepository<CareerPlanFeedback, Long> {

    /** 按规划 ID 查询教师反馈，按创建时间正序 */
    List<CareerPlanFeedback> findByCareerPlanIdOrderByCreatedAtAsc(Long careerPlanId);
}
