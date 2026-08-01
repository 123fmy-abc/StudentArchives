package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学期成绩汇总 Repository
 */
@Repository
public interface SemesterGpaSummaryRepository extends JpaRepository<SemesterGpaSummary, Long> {

    /**
     * 查询学生全部学期成绩汇总（用于累计学分等统计）
     */
    List<SemesterGpaSummary> findByUserId(Long userId);

    /**
     * 查询学生某学期的成绩汇总
     */
    Optional<SemesterGpaSummary> findByUserIdAndSemesterId(Long userId, Long semesterId);
}
