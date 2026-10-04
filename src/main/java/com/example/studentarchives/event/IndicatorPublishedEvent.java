package com.example.studentarchives.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 指标规则版本发布成功事件。
 * <p>
 * 由 {@code AdminIndicatorService.publish} 在事务提交前发布，经
 * {@code IndicatorPublishedEventListener}（{@code @TransactionalEventListener(phase = AFTER_COMMIT)}）
 * 在事务提交后消费，用于自动触发该学期全量评分重算。
 */
@Getter
public class IndicatorPublishedEvent extends ApplicationEvent {

    private final Long schoolId;
    private final Long semesterId;
    private final Long operatorId;

    public IndicatorPublishedEvent(Object source, Long schoolId, Long semesterId, Long operatorId) {
        super(source);
        this.schoolId = schoolId;
        this.semesterId = semesterId;
        this.operatorId = operatorId;
    }
}
