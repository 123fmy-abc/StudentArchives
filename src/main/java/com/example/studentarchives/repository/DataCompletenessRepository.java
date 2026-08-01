package com.example.studentarchives.repository;

import com.example.studentarchives.entity.evaluation.DataCompleteness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据完整度 Repository
 */
@Repository
public interface DataCompletenessRepository extends JpaRepository<DataCompleteness, Long> {

    /**
     * 查询学生某学期各维度的数据完整度
     */
    List<DataCompleteness> findByUserIdAndSemesterId(Long userId, Long semesterId);
}
