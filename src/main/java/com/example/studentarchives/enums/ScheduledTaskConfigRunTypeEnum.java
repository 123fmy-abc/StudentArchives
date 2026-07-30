package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务配置运行类型枚举（对齐 scheduled_task_configs.run_type）
 * <p>
 * 学生档案系统表 scheduled_task_configs.run_type
 * 1=定时自动 2=手动触发
 */
@Getter
@AllArgsConstructor
public enum ScheduledTaskConfigRunTypeEnum {

    AUTO(1, "定时自动"),
    MANUAL(2, "手动触发"),
    ;

    private final int value;
    private final String label;

    public static ScheduledTaskConfigRunTypeEnum of(Integer value) {
        if (value == null) return null;
        for (ScheduledTaskConfigRunTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
