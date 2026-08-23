package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 学院 Repository
 */
@Repository
public interface CollegeRepository extends JpaRepository<College, Long>, JpaSpecificationExecutor<College> {

    List<College> findByIdIn(Collection<Long> ids);

    /**
     * 查询某学校下全部学院（组织架构下钻：schoolId → colleges）
     */
    List<College> findBySchoolId(Long schoolId);
}
