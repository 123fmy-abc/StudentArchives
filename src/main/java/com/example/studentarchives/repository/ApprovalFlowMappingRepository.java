package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalFlowMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程业务映射 Repository（对应表 approval_flow_mappings）
 * <p>
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface ApprovalFlowMappingRepository extends JpaRepository<ApprovalFlowMapping, Long> {

    /** 查询某学校下的全部映射（含未删除） */
    List<ApprovalFlowMapping> findBySchoolId(Long schoolId);

    /** 查询某学校下指定业务类型的全部映射（含未删除） */
    List<ApprovalFlowMapping> findBySchoolIdAndBusinessType(Long schoolId, String businessType);

    /**
     * 软删除映射（deleted_at 置为当前时间，仅命中未删除记录）
     *
     * @return 受影响行数（0 表示映射不存在或已删除）
     */
    @Modifying
    @Query(value = "UPDATE approval_flow_mappings SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL",
            nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
