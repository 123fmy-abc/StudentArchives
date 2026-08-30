package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.approval.ApprovalDelegation;
import com.example.studentarchives.repository.ApprovalDelegationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批委托状态自动流转定时任务（Fmy）
 * <p>
 * 将 approval_delegations.status 按时间物理刷库，使其他模块可直接信任存储状态：
 * <ul>
 *   <li>待生效(0) 到达 start_time → 生效中(1)；</li>
 *   <li>生效中(1) 超过 end_time → 已过期(2)，权限自动归还委托人。</li>
 * </ul>
 * 已取消(3) 为人工置位，不受本任务影响。
 * 复用 {@link org.springframework.scheduling.annotation.EnableScheduling}（SchedulingConfig）调度基建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDelegationScheduler {

    private static final int STATUS_ACTIVE = 1;   // 生效中
    private static final int STATUS_EXPIRED = 2;  // 已过期

    private final ApprovalDelegationRepository approvalDelegationRepository;

    /**
     * 每分钟执行一次委托状态刷库（对齐委托时间按分钟精度生效）
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void transitionDelegationStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 待生效(0) → 生效中(1)：已到开始时间
        List<ApprovalDelegation> toActivate = approvalDelegationRepository
                .findByStatusAndStartTimeLessThanEqual(0, now);
        toActivate.forEach(d -> d.setStatus(STATUS_ACTIVE));
        approvalDelegationRepository.saveAll(toActivate);

        // 生效中(1) → 已过期(2)：已过结束时间
        List<ApprovalDelegation> toExpire = approvalDelegationRepository
                .findByStatusAndEndTimeLessThanEqual(STATUS_ACTIVE, now);
        toExpire.forEach(d -> d.setStatus(STATUS_EXPIRED));
        approvalDelegationRepository.saveAll(toExpire);

        if (!toActivate.isEmpty() || !toExpire.isEmpty()) {
            log.info("审批委托状态刷库完成：待生效→生效中 {} 条，生效中→已过期 {} 条",
                    toActivate.size(), toExpire.size());
        }
    }
}
