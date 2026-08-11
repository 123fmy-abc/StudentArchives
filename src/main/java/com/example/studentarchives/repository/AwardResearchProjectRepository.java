package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardResearchProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 科研之星-科研项目子表 Repository
 */
@Repository
public interface AwardResearchProjectRepository extends JpaRepository<AwardResearchProject, Long> {

    List<AwardResearchProject> findByResearchStarId(Long researchStarId);

    List<AwardResearchProject> findByResearchStarIdIn(List<Long> researchStarIds);
}
