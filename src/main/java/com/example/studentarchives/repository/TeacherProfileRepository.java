package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 教师档案 Repository
 */
@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    Optional<TeacherProfile> findByUserId(Long userId);

    List<TeacherProfile> findByUserIdIn(Collection<Long> userIds);
}