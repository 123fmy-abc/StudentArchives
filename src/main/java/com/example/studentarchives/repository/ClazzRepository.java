package com.example.studentarchives.repository;

import com.example.studentarchives.entity.org.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 班级 Repository
 */
@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {

    List<Clazz> findByGrade(String grade);

    List<Clazz> findByIdIn(Collection<Long> ids);
}
