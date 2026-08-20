package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 奖项报名 Repository
 */
@Repository
public interface AwardApplicationRepository extends JpaRepository<AwardApplication, Long> {

    /** 查询学生全部奖项记录（用于活动列表聚合 + Java 侧筛选） */
    List<AwardApplication> findByUserId(Long userId);

    /** 软删除（通过 native query 绕过 updatable=false 限制） */
    @Modifying
    @Query(value = "UPDATE award_applications SET deleted_at = :deletedAt WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 统计某学校某学期指定状态的奖项申请数量
     */
    @Query("SELECT COUNT(a) FROM AwardApplication a WHERE a.schoolId = :schoolId AND a.semesterId = :semesterId AND a.status = :status")
    Long countBySchoolIdAndSemesterIdAndStatus(@Param("schoolId") Long schoolId,
                                                @Param("semesterId") Long semesterId,
                                                @Param("status") Integer status);

    /**
     * 批量查询多个学生某学期指定状态的奖项申请
     */
    @Query("SELECT a FROM AwardApplication a WHERE a.userId IN :userIds AND a.semesterId = :semesterId AND a.status = :status")
    List<AwardApplication> findByUserIdInAndSemesterIdAndStatus(@Param("userIds") Collection<Long> userIds,
                                                                 @Param("semesterId") Long semesterId,
                                                                 @Param("status") Integer status);
}
