package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.DataCompleteness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 数据完整度补充 Repository（对应表 data_completeness）
 * <p>
 * 管理端统计看板数据完整度 KPI：按学校所有学生（users.school_id）某学期
 * 的完整度记录做单条 AVG 聚合，避免全量载入内存。
 */
@Repository
public interface AdminDataCompletenessRepository extends JpaRepository<DataCompleteness, Long> {

    /**
     * 计算某学校某学期学生数据完整度平均值（0-100）
     */
    @Query("SELECT AVG(d.completenessRate) FROM DataCompleteness d " +
            "WHERE (:semesterId IS NULL OR d.semesterId = :semesterId) " +
            "AND d.userId IN (SELECT u.id FROM User u WHERE u.schoolId = :schoolId)")
    Double avgCompletenessBySchool(@Param("schoolId") Long schoolId,
                                   @Param("semesterId") Long semesterId);
}
