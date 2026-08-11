package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardResearchStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 科研之星奖项明细 Repository
 */
@Repository
public interface AwardResearchStarRepository extends JpaRepository<AwardResearchStar, Long> {

    Optional<AwardResearchStar> findByApplicationId(Long applicationId);

    List<AwardResearchStar> findByApplicationIdIn(List<Long> applicationIds);
}
