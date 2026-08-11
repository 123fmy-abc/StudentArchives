package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 组织履历扩展表 Repository
 */
@Repository
public interface ArchiveOrganizationRepository extends JpaRepository<ArchiveOrganization, Long> {

    Optional<ArchiveOrganization> findByArchiveId(Long archiveId);
}
