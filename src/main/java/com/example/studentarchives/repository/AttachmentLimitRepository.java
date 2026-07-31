package com.example.studentarchives.repository;

import com.example.studentarchives.entity.file.AttachmentLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 附件限制 Repository
 */
@Repository
public interface AttachmentLimitRepository extends JpaRepository<AttachmentLimit, Long> {

    /**
     * 查询指定学校和档案类型的附件限制配置
     */
    @Query("SELECT a FROM AttachmentLimit a WHERE a.schoolId = :schoolId AND a.archiveType = :archiveType AND a.status = 1")
    Optional<AttachmentLimit> findBySchoolIdAndArchiveType(@Param("schoolId") Long schoolId, @Param("archiveType") String archiveType);
}
