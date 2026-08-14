package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 学生档案 Repository
 */

@Repository
//interface: 这是一个接口，不需要写实现类。
//泛型参数 <StudentProfile, Long>:指定该 Repository 操作的实体类（对应数据库中的表）
//Long: 指定主键 ID 的数据类型是 Long（通常对应数据库的 BIGINT）。
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    List<StudentProfile> findByUserIdIn(Collection<Long> userIds);

    List<StudentProfile> findByClassIdIn(Collection<Long> classIds);

    /**
     * 查询某班级下全部学生（评分重算 targetType=2 指定班级时使用）
     */
    List<StudentProfile> findByClassId(Long classId);

    /**
     * 查询某学校下全部学生（评分重算 targetType=4 全量 / targetType=3 指定学期时使用）。
     * 学生归属学校由 users.school_id 决定。
     */
    @Query("SELECT sp FROM StudentProfile sp WHERE sp.userId IN (SELECT u.id FROM User u WHERE u.schoolId = :schoolId)")
    List<StudentProfile> findBySchoolId(@Param("schoolId") Long schoolId);
}