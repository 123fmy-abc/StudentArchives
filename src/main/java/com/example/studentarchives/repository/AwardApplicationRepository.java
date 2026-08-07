package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 奖项申请 Repository
 */
@Repository
public interface AwardApplicationRepository extends JpaRepository<AwardApplication, Long> {

    /**
     * 查询学生全部奖项申请（用于简历导出聚合获奖经历）
     */
    List<AwardApplication> findByUserId(Long userId);
}
