package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.SemesterGpaSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 学期成绩汇总补充 Repository（对应表 semester_gpa_summaries）
 * <p>
 * 管理端成绩导入（班级/专业内排名）与统计模块（平均绩点）专用批量查询。
 */
@Repository
public interface AdminSemesterGpaSummaryRepository extends JpaRepository<SemesterGpaSummary, Long> {

    /** 查询某学期某班级全部学生的成绩汇总（用于班级内排名） */
    List<SemesterGpaSummary> findBySemesterIdAndClassId(Long semesterId, Long classId);

    /** 查询某学期某专业全部学生的成绩汇总（用于专业内排名） */
    List<SemesterGpaSummary> findBySemesterIdAndMajorId(Long semesterId, Long majorId);

    /** 批量查询多个学生某学期的成绩汇总（统计模块平均绩点） */
    List<SemesterGpaSummary> findBySemesterIdAndUserIdIn(Long semesterId, Collection<Long> userIds);
}
