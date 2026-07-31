package com.example.studentarchives.repository;

import com.example.studentarchives.entity.file.AttachmentRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 附件关联 Repository
 * <p>
 * 对应表 file_uploads，管理所有文件上传记录。
 */
@Repository
public interface AttachmentRelationRepository extends JpaRepository<AttachmentRelation, Long> {

    /**
     * 根据 ID 和用户 ID 查询附件
     */
    Optional<AttachmentRelation> findByIdAndUserId(Long id, Long userId);

    /**
     * 统计用户指定类别的附件数量
     */
    long countByUserIdAndFileCategory(Long userId, String fileCategory);

    /**
     * 软删除附件记录（通过 native query 绕过 updatable=false 限制）
     */
    @Modifying
    @Query(value = "UPDATE file_uploads SET deleted_at = :deletedAt, deleted_by = :deletedBy WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt, @Param("deletedBy") Long deletedBy);
}
