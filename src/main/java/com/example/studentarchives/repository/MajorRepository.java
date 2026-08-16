package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 专业 Repository
 */
@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {

    List<Major> findByIdIn(Collection<Long> ids);

    /**
     * 查询某学院下全部专业（操作日志组织维度下钻：collegeId → majors）
     */
    List<Major> findByCollegeIdIn(Collection<Long> collegeIds);
}
