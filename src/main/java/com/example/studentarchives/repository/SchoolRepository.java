package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 学校 Repository
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByIdIn(Collection<Long> ids);
}
