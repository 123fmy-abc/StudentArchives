package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 组织范围类型枚举
 */
@Getter
@AllArgsConstructor
public enum ScopeTypeEnum {

    SCHOOL(1, "学校"),
    COLLEGE(2, "学院"),
    MAJOR(3, "专业"),
    CLASS(4, "班级"),
    ;

    private final int value;
    private final String label;

    public static ScopeTypeEnum of(Integer value) {
        if (value == null) return null;
        for (ScopeTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
