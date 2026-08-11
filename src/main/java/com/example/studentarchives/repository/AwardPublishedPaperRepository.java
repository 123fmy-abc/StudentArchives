package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardPublishedPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 科研之星-发表论文子表 Repository
 */
@Repository
public interface AwardPublishedPaperRepository extends JpaRepository<AwardPublishedPaper, Long> {

    List<AwardPublishedPaper> findByResearchStarId(Long researchStarId);

    List<AwardPublishedPaper> findByResearchStarIdIn(List<Long> researchStarIds);
}
