package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardCompetitionStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 竞赛之星奖项明细 Repository
 */
@Repository
public interface AwardCompetitionStarRepository extends JpaRepository<AwardCompetitionStar, Long> {

    Optional<AwardCompetitionStar> findByApplicationId(Long applicationId);

    List<AwardCompetitionStar> findByApplicationIdIn(List<Long> applicationIds);
}
