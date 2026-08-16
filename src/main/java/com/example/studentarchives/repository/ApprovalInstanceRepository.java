package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 审批流程实例 Repository（对应表 approval_instances）
 * <p>
 * 供审批流程配置模块校验：流程是否已被审批实例引用、是否存在审批中实例。
 */
@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

    /** 是否存在引用该流程的审批实例（含已完成的，未删除） */
    boolean existsByFlowId(Long flowId);

    /** 是否存在引用该流程且处于指定状态的审批实例（未删除） */
    boolean existsByFlowIdAndStatus(Long flowId, Integer status);
}
