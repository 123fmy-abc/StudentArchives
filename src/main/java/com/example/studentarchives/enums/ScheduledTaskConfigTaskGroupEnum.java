package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 定时任务配置任务分组枚举（对齐 scheduled_task_configs.task_group）
 * <p>
 * 学生档案系统表 scheduled_task_configs.task_group
 * system/data/notification/cleanup
 */
@Getter
@AllArgsConstructor
public enum ScheduledTaskConfigTaskGroupEnum {

    SYSTEM("system", "系统"),
    DATA("data", "数据"),
    NOTIFICATION("notification", "通知"),
    CLEANUP("cleanup", "清理"),
    ;

    private final String value;
    private final String label;

    public static ScheduledTaskConfigTaskGroupEnum of(String value) {
        if (value == null) return null;
        for (ScheduledTaskConfigTaskGroupEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
