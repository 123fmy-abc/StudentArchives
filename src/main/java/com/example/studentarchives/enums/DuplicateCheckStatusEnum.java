package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 重复检测状态枚举（对齐 duplicate_checks.duplicate_check_status）
 * <p>
 * 学生档案系统表 duplicate_checks.duplicate_check_status
 * 0=未检测 1=检测中 2=疑似重复 3=已排除
 */
@Getter
@AllArgsConstructor
public enum DuplicateCheckStatusEnum {

    NOT_CHECKED(0, "未检测"),
    CHECKING(1, "检测中"),
    SUSPECTED(2, "疑似重复"),
    EXCLUDED(3, "已排除"),
    ;

    private final int value;
    private final String label;

    public static DuplicateCheckStatusEnum of(Integer value) {
        if (value == null) return NOT_CHECKED;
        for (DuplicateCheckStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return NOT_CHECKED;
    }
}
