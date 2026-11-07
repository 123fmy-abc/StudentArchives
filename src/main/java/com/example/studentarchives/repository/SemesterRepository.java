package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 学期 Repository
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long>, JpaSpecificationExecutor<Semester> {

    /**
     * 查询指定学校下所有启用的学期，按 start_date 倒序
     */
    @Query("SELECT s FROM Semester s WHERE s.schoolId = :schoolId AND s.status = 1 ORDER BY s.startDate DESC")
    List<Semester> findActiveBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 查询指定学校下当前生效的学期（is_current=1 且启用）
     */
    @Query("SELECT s FROM Semester s WHERE s.schoolId = :schoolId AND s.isCurrent = 1 AND s.status = 1")
    Optional<Semester> findCurrentBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 查询指定学校下所有学期（用于名称唯一校验、日期重叠校验与设置当前学期）
     */
    List<Semester> findBySchoolId(Long schoolId);

    /**
     * 同学校下按名称查询（用于名称唯一性校验）
     */
    Optional<Semester> findBySchoolIdAndName(Long schoolId, String name);

    /**
     * 查询指定学校下覆盖某日期的启用学期（start_date ≤ date ≤ end_date），
     * 用于成长时间轴事件按发生时间（eventAt）自动归集学期。
     * 建学期时已校验同校学期日期不重叠，正常最多命中一条，此处取 start_date 最晚的一条。
     */
    @Query("SELECT s FROM Semester s WHERE s.schoolId = :schoolId AND s.status = 1 "
            + "AND s.startDate <= :date AND s.endDate >= :date ORDER BY s.startDate DESC")
    List<Semester> findActiveBySchoolIdAndDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);
}
