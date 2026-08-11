package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveInnovation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 创新创业扩展表 Repository
 */
@Repository
public interface ArchiveInnovationRepository extends JpaRepository<ArchiveInnovation, Long> {

    Optional<ArchiveInnovation> findByArchiveId(Long archiveId);
}
