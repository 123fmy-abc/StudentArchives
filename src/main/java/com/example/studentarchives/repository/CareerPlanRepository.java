package com.example.studentarchives.repository;

import com.example.studentarchives.entity.career.CareerPlan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 职业规划 Repository（对应表 career_plans）
 */
@Repository
public interface CareerPlanRepository extends JpaRepository<CareerPlan, Long> {

    /** 按 ID + 归属用户查询（归属校验） */
    Optional<CareerPlan> findByIdAndUserId(Long id, Long userId);

    /** 查询用户全部规划（导出等场景使用） */
    List<CareerPlan> findByUserId(Long userId);

    /** 分页查询用户全部规划（按创建时间倒序） */
    List<CareerPlan> findByUserId(Long userId, Pageable pageable);

    /** 统计用户规划总数 */
    long countByUserId(Long userId);

    /** 分页查询用户指定学期规划 */
    List<CareerPlan> findByUserIdAndSemesterId(Long userId, Long semesterId, Pageable pageable);

    /** 统计用户指定学期规划数 */
    long countByUserIdAndSemesterId(Long userId, Long semesterId);

    /** 取用户指定学期最近一条规划（复制场景） */
    Optional<CareerPlan> findFirstByUserIdAndSemesterIdOrderByIdDesc(Long userId, Long semesterId);
}
