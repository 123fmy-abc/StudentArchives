package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardInnovationStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 双创之星奖项明细 Repository
 */
@Repository
public interface AwardInnovationStarRepository extends JpaRepository<AwardInnovationStar, Long> {

    Optional<AwardInnovationStar> findByApplicationId(Long applicationId);

    List<AwardInnovationStar> findByApplicationIdIn(List<Long> applicationIds);
}
