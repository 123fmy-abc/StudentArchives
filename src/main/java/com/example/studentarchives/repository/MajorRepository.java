package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 专业 Repository
 */
@Repository
public interface MajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {

    List<Major> findByIdIn(Collection<Long> ids);

    /**
     * 查询某学院下全部专业（操作日志组织维度下钻：collegeId → majors）
     */
    List<Major> findByCollegeIdIn(Collection<Long> collegeIds);

    /**
     * 学院内按专业代码查询（用于唯一性校验，deleted_at IS NULL 由 @SQLRestriction 保证）
     */
    Optional<Major> findByCollegeIdAndCode(Long collegeId, String code);
}
