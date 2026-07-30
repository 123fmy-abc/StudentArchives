package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 导入任务状态枚举（对齐 import_tasks.import_status）
 * <p>
 * 学生档案系统表 import_tasks.import_status
 * 0=导入中 1=完成 2=失败
 */
@Getter
@AllArgsConstructor
public enum ImportStatusEnum {

    IMPORTING(0, "导入中"),
    COMPLETED(1, "完成"),
    FAILED(2, "失败"),
    ;

    private final int value;
    private final String label;

    public static ImportStatusEnum of(Integer value) {
        if (value == null) return IMPORTING;
        for (ImportStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return IMPORTING;
    }
}
