package com.example.studentarchives.repository;

import com.example.studentarchives.entity.message.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告/信息发布 Repository
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {

    List<Announcement> findBySchoolIdAndStatusOrderByPublishedAtDesc(Long schoolId, Integer status);

    /**
     * 软删除公告（deleted_at 置为当前时间，仅命中未删除记录）
     */
    @Modifying
    @Query(value = "UPDATE announcements SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}