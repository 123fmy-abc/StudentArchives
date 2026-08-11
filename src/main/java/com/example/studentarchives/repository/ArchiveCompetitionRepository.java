package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 学科竞赛扩展表 Repository
 */
@Repository
public interface ArchiveCompetitionRepository extends JpaRepository<ArchiveCompetition, Long> {

    Optional<ArchiveCompetition> findByArchiveId(Long archiveId);
}
