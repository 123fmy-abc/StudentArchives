package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardSoftwareCopyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 科研之星-软件著作权子表 Repository
 */
@Repository
public interface AwardSoftwareCopyrightRepository extends JpaRepository<AwardSoftwareCopyright, Long> {

    List<AwardSoftwareCopyright> findByResearchStarId(Long researchStarId);

    List<AwardSoftwareCopyright> findByResearchStarIdIn(List<Long> researchStarIds);
}
