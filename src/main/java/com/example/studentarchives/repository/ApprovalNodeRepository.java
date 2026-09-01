package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批节点 Repository（对应表 approval_nodes）
 * <p>
 * 供教师端「待审核任务模块」（《教师端接口文档》四）记录节点动作与查询审批历史。
 * 实体上的 {@code @SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)} 生效，
 * 查询结果自动过滤已软删除记录。
 */
@Repository
public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long> {

    /** 查询某审批实例下的全部节点，按步骤号升序（用于审批历史展示） */
    List<ApprovalNode> findByInstanceIdOrderByStepNoAsc(Long instanceId);
}