package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardResearchStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 科研之星奖项明细 Repository
 */
@Repository
public interface AwardResearchStarRepository extends JpaRepository<AwardResearchStar, Long> {

    /**
     * 根据申请 ID 列表查询科研之星奖项明细
     */
    List<AwardResearchStar> findByApplicationIdIn(List<Long> applicationIds);
}
