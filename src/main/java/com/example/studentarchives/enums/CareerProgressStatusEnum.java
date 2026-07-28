package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 职业规划进度状态枚举
 * <p>
 * 对齐 学生档案系统表 career_goals.status / career_actions.status
 * <p>
 * 0=未开始 1=进行中 2=已完成
 * <p>
 * 适用表：career_goals.status、career_actions.status
 */
@Getter
@AllArgsConstructor
public enum CareerProgressStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    ;

    private final int value;
    private final String label;

    public static CareerProgressStatusEnum of(Integer value) {
        if (value == null) return NOT_STARTED;
        for (CareerProgressStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return NOT_STARTED;
    }

    /** 是否已完成 */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /** 是否进行中（含未开始和进行中） */
    public boolean isInProgress() {
        return this == NOT_STARTED || this == IN_PROGRESS;
    }
}
