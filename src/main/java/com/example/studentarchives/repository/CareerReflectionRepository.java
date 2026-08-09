package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerReflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 职业规划阶段反思 Repository（对应表 career_reflections）
 */
@Repository
public interface CareerReflectionRepository extends JpaRepository<CareerReflection, Long> {

    /** 按规划 ID 查询反思，按创建时间正序 */
    List<CareerReflection> findByCareerPlanIdOrderByCreatedAtAsc(Long careerPlanId);
}
