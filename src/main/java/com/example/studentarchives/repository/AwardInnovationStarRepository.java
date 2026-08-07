package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardInnovationStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 双创之星奖项明细 Repository
 */
@Repository
public interface AwardInnovationStarRepository extends JpaRepository<AwardInnovationStar, Long> {

    /**
     * 根据申请 ID 列表查询双创之星奖项明细
     */
    List<AwardInnovationStar> findByApplicationIdIn(List<Long> applicationIds);
}
