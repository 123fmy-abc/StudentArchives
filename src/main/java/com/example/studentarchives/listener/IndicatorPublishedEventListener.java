package com.example.studentarchives.listener;

import com.example.studentarchives.event.IndicatorPublishedEvent;
import com.example.studentarchives.service.Fmy.AdminScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 指标规则版本发布成功事件监听器。
 * <p>
 * 在 {@code AdminIndicatorService.publish} 的事务提交后（AFTER_COMMIT）触发，
 * 调用 {@link AdminScoreService#triggerSemesterRecalculation} 自动重算该学期所有学生的画像评分，
 * 避免发布新规则后存量学生仍沿用旧版本分数。发布事务回滚则本监听器不会执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorPublishedEventListener {

    private final AdminScoreService adminScoreService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIndicatorPublished(IndicatorPublishedEvent event) {
        Long semesterId = event.getSemesterId();
        if (semesterId == null) {
            log.info("指标发布未绑定学期，跳过自动评分重算: schoolId={}", event.getSchoolId());
            return;
        }
        try {
            adminScoreService.triggerSemesterRecalculation(event.getSchoolId(), semesterId, event.getOperatorId());
        } catch (Exception e) {
            log.warn("指标发布后自动评分重算触发失败: schoolId={}, semesterId={}, err={}",
                    event.getSchoolId(), semesterId, e.getMessage());
        }
    }
}
