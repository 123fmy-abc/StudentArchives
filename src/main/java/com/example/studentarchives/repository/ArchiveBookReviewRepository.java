package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.ArchiveBookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 图书心得扩展表 Repository
 */
@Repository
public interface ArchiveBookReviewRepository extends JpaRepository<ArchiveBookReview, Long> {

    Optional<ArchiveBookReview> findByArchiveId(Long archiveId);
}
