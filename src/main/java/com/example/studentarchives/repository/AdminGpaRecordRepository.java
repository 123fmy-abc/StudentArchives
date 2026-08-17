package com.example.studentarchives.repository;

import com.example.studentarchives.entity.grade.GpaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 课程成绩（GPA）记录补充 Repository（对应表 gpa_records）
 * <p>
 * 管理端成绩导入与统计模块专用。gpa_records 无软删除列（BaseEntityNoUpdate），
 * 覆盖导入通过 find + deleteAll 物理删除既有记录。
 */
@Repository
public interface AdminGpaRecordRepository extends JpaRepository<GpaRecord, Long> {

    /** 查询学生某学期某课程的全部成绩记录（覆盖模式下的去重判定） */
    List<GpaRecord> findByUserIdAndSemesterIdAndCourseCode(Long userId, Long semesterId, String courseCode);

    /** 批量查询多个学生某学期的课程成绩（统计模块实时聚合） */
    List<GpaRecord> findByUserIdInAndSemesterId(Collection<Long> userIds, Long semesterId);
}
