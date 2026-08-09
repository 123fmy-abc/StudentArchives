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

    /**
     * 根据科研之星 ID 列表查询科研项目明细
     */
    List<AwardResearchProject> findByResearchStarIdIn(List<Long> researchStarIds);
}
