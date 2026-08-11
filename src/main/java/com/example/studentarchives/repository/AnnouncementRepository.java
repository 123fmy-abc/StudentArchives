package com.example.studentarchives.repository;

import com.example.studentarchives.entity.message.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公告/信息发布 Repository
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findBySchoolIdAndStatusOrderByPublishedAtDesc(Long schoolId, Integer status);
}
