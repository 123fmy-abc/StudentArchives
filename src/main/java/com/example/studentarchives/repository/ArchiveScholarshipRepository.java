package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveScholarship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 奖学金扩展表 Repository
 */
@Repository
public interface ArchiveScholarshipRepository extends JpaRepository<ArchiveScholarship, Long> {

    Optional<ArchiveScholarship> findByArchiveId(Long archiveId);
}
