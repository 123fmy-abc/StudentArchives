package com.example.studentarchives.repository;

import com.example.studentarchives.entity.approval.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审批委托 Repository（对应表 approval_delegations）
 * <p>
 * 支撑教师端审批委托模块（《教师端接口文档》十一、审批委托模块）。
 * 委托状态：0=待生效 1=生效中 2=已过期 3=已取消。
 */
@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {

    /** 我委托的（委托人视角），按创建时间倒序 */
    List<ApprovalDelegation> findByDelegatorIdOrderByCreatedAtDesc(Long delegatorId);

    /** 委托给我的（受托人视角），按创建时间倒序 */
    List<ApprovalDelegation> findByDelegateeIdOrderByCreatedAtDesc(Long delegateeId);

    /** 按 ID + 委托人查询（取消委托时归属校验） */
    Optional<ApprovalDelegation> findByIdAndDelegatorId(Long id, Long delegatorId);

    /**
     * 查询同一委托人时段重叠的生效中/待生效委托（status IN (0,1)）。
     * <p>
     * 重叠判定：既有委托开始时间 < 新结束时间 且 既有委托结束时间 > 新开始时间
     * （两端均为开区间，恰好首尾相接视为不重叠）。
     */
    @Query("SELECT d FROM ApprovalDelegation d WHERE d.delegatorId = :delegatorId "
            + "AND d.status IN (0, 1) "
            + "AND d.startTime < :endTime AND d.endTime > :startTime")
    List<ApprovalDelegation> findOverlapping(
            @Param("delegatorId") Long delegatorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /** 待生效(0)且已到开始时间 → 定时任务刷为生效中(1) */
    List<ApprovalDelegation> findByStatusAndStartTimeLessThanEqual(Integer status, LocalDateTime time);

    /** 生效中(1)且已过结束时间 → 定时任务刷为已过期(2) */
    List<ApprovalDelegation> findByStatusAndEndTimeLessThanEqual(Integer status, LocalDateTime time);
}
