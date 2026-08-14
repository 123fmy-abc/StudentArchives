package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 班级 Repository
 */
@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {

    /**
     * 查询某专业下全部班级（评分重算 targetType=5 指定专业时解析班级）
     */
    List<Clazz> findByMajorId(Long majorId);
}
