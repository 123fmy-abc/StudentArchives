package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 导出任务状态枚举（对齐 export_tasks.status）
 * <p>
 * 学生档案系统表 export_tasks.status
 * 0=待执行 1=执行中 2=完成 3=失败
 */
@Getter
@AllArgsConstructor
public enum ExportTaskStatusEnum {

    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败"),
    ;

    private final int value;
    private final String label;

    public static ExportTaskStatusEnum of(Integer value) {
        if (value == null) return PENDING;
        for (ExportTaskStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return PENDING;
    }
}
