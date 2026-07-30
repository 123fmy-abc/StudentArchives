package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务执行状态枚举（对齐 scheduled_tasks.status）
 * <p>
 * 学生档案系统表 scheduled_tasks.status
 * 0=计算中 1=完成 2=失败
 */
@Getter
@AllArgsConstructor
public enum ScheduledTaskStatusEnum {

    RUNNING(0, "计算中"),
    COMPLETED(1, "完成"),
    FAILED(2, "失败"),
    ;

    private final int value;
    private final String label;

    public static ScheduledTaskStatusEnum of(Integer value) {
        if (value == null) return RUNNING;
        for (ScheduledTaskStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return RUNNING;
    }
}
