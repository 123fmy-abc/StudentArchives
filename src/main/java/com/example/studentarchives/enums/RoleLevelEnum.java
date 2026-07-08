package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色层级枚举
 */
@Getter
@AllArgsConstructor
public enum RoleLevelEnum {

    SYSTEM(0, "系统"),
    STUDENT(1, "学生"),
    TEACHER(2, "科任教师"),
    COUNSELOR(3, "辅导员"),
    DEPARTMENT_HEAD(4, "系主任"),
    COLLEGE_DEAN(5, "院长"),
    PRINCIPAL(6, "校长"),
    CUSTOM(7, "自定义"),
    ;

    private final int value;
    private final String label;

    public static RoleLevelEnum of(Integer value) {
        if (value == null) return SYSTEM;
        for (RoleLevelEnum e : values()) {
            if (e.value == value) return e;
        }
        return CUSTOM;
    }
}
