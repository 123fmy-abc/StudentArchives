package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务触发类型枚举（对齐 scheduled_tasks.trigger_type）
 * <p>
 * 学生档案系统表 scheduled_tasks.trigger_type
 * 1=手动触发 2=系统自动/定时任务
 */
@Getter
@AllArgsConstructor
public enum ScheduledTaskTriggerTypeEnum {

    MANUAL(1, "手动触发"),
    AUTO(2, "系统自动/定时任务"),
    ;

    private final int value;
    private final String label;

    public static ScheduledTaskTriggerTypeEnum of(Integer value) {
        if (value == null) return null;
        for (ScheduledTaskTriggerTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
