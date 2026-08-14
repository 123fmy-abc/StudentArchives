package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.GpaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程成绩（GPA）记录 Repository（对应表 gpa_records）
 * <p>
 * 供评分重算模块读取课程成绩类数据源：required_course_scores（必修课平均分）、
 * course_excellent_rate（必修课优良率）等。
 */
@Repository
public interface GpaRecordRepository extends JpaRepository<GpaRecord, Long> {

    /**
     * 查询学生全部课程成绩记录（按课程代码排序）
     */
    List<GpaRecord> findByUserIdOrderByCourseCodeAsc(Long userId);

    /**
     * 查询学生某学期的课程成绩记录
     */
    List<GpaRecord> findByUserIdAndSemesterIdOrderByCourseCodeAsc(Long userId, Long semesterId);
}
