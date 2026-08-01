package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 班级 Repository
 */
@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {
}
