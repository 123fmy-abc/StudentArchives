package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 统一待审批任务 Repository（对应表 pending_approvals）
 * <p>
 * 教师首页数据概览按 {@code auditor_id} 聚合当前教师的待办（状态=1 待审批），
 * 按 {@code approvable_type}（Archive / AwardApplication / CareerPlan）分组统计。
 */
@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {

    /** 查询审批人当前待审批任务（status=1 待审批），按提交时间正序（先提交先审） */
    List<PendingApproval> findByAuditorIdAndStatusOrderBySubmittedAtAsc(Long auditorId, Integer status);
}
