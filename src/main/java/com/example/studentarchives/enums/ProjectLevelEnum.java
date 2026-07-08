package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目级别枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectLevelEnum {

    NATIONAL("national", "国家级"),
    PROVINCIAL("provincial", "省部级"),
    SCHOOL("school", "校级"),
    COLLEGE("college", "院级"),
    ;

    private final String value;
    private final String label;

    public static ProjectLevelEnum of(String value) {
        if (value == null) return null;
        for (ProjectLevelEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
