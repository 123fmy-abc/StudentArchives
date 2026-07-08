package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举（0=禁用/草稿/失败/未读，1=启用/正常/待审批/成功/已读）
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    ;

    private final int value;
    private final String label;

    public static StatusEnum of(Integer value) {
        if (value == null) return DISABLED;
        for (StatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return DISABLED;
    }
}
