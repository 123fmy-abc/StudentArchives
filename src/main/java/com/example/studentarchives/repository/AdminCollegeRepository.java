package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学院补充 Repository（对应表 colleges）
 * <p>
 * 管理端档案汇总/统计模块需要按学校列出学院（CollegeRepository 仅提供按 ID 查询）。
 */
@Repository
public interface AdminCollegeRepository extends JpaRepository<College, Long> {

    /** 查询学校下全部学院 */
    List<College> findBySchoolId(Long schoolId);
}
