package com.example.studentarchives.repository;

import com.example.studentarchives.entity.file.AttachmentRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
     * 根据业务类型和业务 ID 查询附件（按 sortOrder 正序）
     */
    List<AttachmentRelation> findByBizTypeAndBizIdOrderBySortOrderAsc(String bizType, Long bizId);

    /**
     * 根据业务类型和业务 ID 集合批量查询附件（用于职业规划详情中多个行动的文件聚合）
     */
    List<AttachmentRelation> findByBizTypeAndBizIdIn(String bizType, Collection<Long> bizIds);

    /**
     * 统计用户指定类别的附件数量
     */
    long countByUserIdAndFileCategory(Long userId, String fileCategory);

    /**
     * 软删除附件记录（通过 native query 绕过 updatable=false 限制）
     * 同时把 file_status 置为 3（已删除），防止已删记录被再次绑定。
     */
    @Modifying
    @Query(value = "UPDATE file_uploads SET deleted_at = :deletedAt, deleted_by = :deletedBy, file_status = 3 " +
            "WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt, @Param("deletedBy") Long deletedBy);
}
