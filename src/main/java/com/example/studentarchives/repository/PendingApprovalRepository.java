package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 待审核任务 Repository（对应表 pending_approvals）
 * <p>
 * 供教师端「待审核任务模块」（《教师端接口文档》四）查询待办与已处理任务。
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {

    /** 查询某学校下指定状态的待办任务（用于列表基础查询，后续在 Service 层按教师范围过滤） */
    List<PendingApproval> findByStatusAndSchoolId(Integer status, Long schoolId);

    /** 查询某学校下指定审批人 + 状态的待办任务 */
    List<PendingApproval> findByAuditorIdAndStatusAndSchoolId(Long auditorId, Integer status, Long schoolId);

    /** 按模型类型 + 模型 ID + 状态查询待办（幂等/去重校验用） */
    Optional<PendingApproval> findTopByApprovableTypeAndApprovableIdAndStatusOrderByIdDesc(
            String approvableType, Long approvableId, Integer status);
}