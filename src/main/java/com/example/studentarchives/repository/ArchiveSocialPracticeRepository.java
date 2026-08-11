package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveSocialPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 社会实践扩展表 Repository
 */
@Repository
public interface ArchiveSocialPracticeRepository extends JpaRepository<ArchiveSocialPractice, Long> {

    Optional<ArchiveSocialPractice> findByArchiveId(Long archiveId);
}
