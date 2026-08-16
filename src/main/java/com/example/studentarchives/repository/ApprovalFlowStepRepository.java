package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalFlowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程步骤 Repository（对应表 approval_flow_steps）
 * <p>
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface ApprovalFlowStepRepository extends JpaRepository<ApprovalFlowStep, Long> {

    /** 查询某流程下的全部步骤，按 stepNo 升序 */
    List<ApprovalFlowStep> findByFlowIdOrderByStepNoAsc(Long flowId);

    /**
     * 软删除单个步骤（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示步骤不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE approval_flow_steps SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 软删除某流程的全部步骤（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数
     */
    @Modifying
    @Query(value = "UPDATE approval_flow_steps SET deleted_at = :deletedAt WHERE flow_id = :flowId AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteByFlowId(@Param("flowId") Long flowId, @Param("deletedAt") LocalDateTime deletedAt);
}
