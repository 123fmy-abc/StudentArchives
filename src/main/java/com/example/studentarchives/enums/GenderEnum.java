package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int value;
    private final String label;

    public static GenderEnum of(Integer value) {
        if (value == null) return UNKNOWN;
        for (GenderEnum e : values()) {
            if (e.value == value) return e;
        }
        return UNKNOWN;
    }
}
