package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveResearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 学术研究扩展表 Repository
 */
@Repository
public interface ArchiveResearchRepository extends JpaRepository<ArchiveResearch, Long> {

    Optional<ArchiveResearch> findByArchiveId(Long archiveId);
}
