package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
