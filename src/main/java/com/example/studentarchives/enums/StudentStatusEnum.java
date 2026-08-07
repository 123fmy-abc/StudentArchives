package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学生状态枚举（对应 student_profiles.student_status）
 */
@Getter
@AllArgsConstructor
public enum StudentStatusEnum {

    CURRENT("current", "在校生"),
    FRESH_GRADUATE("fresh_graduate", "应届毕业生"),
    GRADUATED("graduated", "已毕业"),
    ;

    private final String value;
    private final String label;

    public static StudentStatusEnum of(String value) {
        if (value == null) return null;
        for (StudentStatusEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
