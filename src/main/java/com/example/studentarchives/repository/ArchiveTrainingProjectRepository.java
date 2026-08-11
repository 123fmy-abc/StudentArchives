package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveTrainingProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 实训项目扩展表 Repository
 */
@Repository
public interface ArchiveTrainingProjectRepository extends JpaRepository<ArchiveTrainingProject, Long> {

    Optional<ArchiveTrainingProject> findByArchiveId(Long archiveId);
}
