package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程配置 Repository（对应表 approval_flows）
 * <p>
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {

    /** 查询某学校下的全部流程（含禁用、未删除） */
    List<ApprovalFlow> findBySchoolId(Long schoolId);

    /** 查询某学校下指定适用类型的全部流程（含禁用、未删除） */
    List<ApprovalFlow> findBySchoolIdAndApplicableType(Long schoolId, String applicableType);

    /**
     * 软删除流程（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示流程不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE approval_flows SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
