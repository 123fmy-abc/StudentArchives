package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学期 Repository
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    /**
     * 查询指定学校下所有启用的学期，按 start_date 倒序
     */
    @Query("SELECT s FROM Semester s WHERE s.schoolId = :schoolId AND s.status = 1 ORDER BY s.startDate DESC")
    List<Semester> findActiveBySchoolId(@Param("schoolId") Long schoolId);
}
