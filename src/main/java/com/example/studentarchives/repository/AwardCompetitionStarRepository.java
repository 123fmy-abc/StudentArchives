package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardCompetitionStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 竞赛之星奖项明细 Repository
 */
@Repository
public interface AwardCompetitionStarRepository extends JpaRepository<AwardCompetitionStar, Long> {

    /**
     * 根据申请 ID 列表查询竞赛奖项明细
     */
    List<AwardCompetitionStar> findByApplicationIdIn(List<Long> applicationIds);
}
