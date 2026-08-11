package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveInternship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 实习经历扩展表 Repository
 */
@Repository
public interface ArchiveInternshipRepository extends JpaRepository<ArchiveInternship, Long> {

    Optional<ArchiveInternship> findByArchiveId(Long archiveId);
}
