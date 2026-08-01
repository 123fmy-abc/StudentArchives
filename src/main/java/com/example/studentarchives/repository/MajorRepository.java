package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 专业 Repository
 */
@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
}
