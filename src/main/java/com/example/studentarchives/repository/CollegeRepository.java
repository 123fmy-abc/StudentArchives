package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 学院 Repository
 */
@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {

    List<College> findByIdIn(Collection<Long> ids);
}
