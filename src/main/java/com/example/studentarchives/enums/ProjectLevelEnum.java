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

    private final String code;
    private final String name;

    public static ProjectLevelEnum of(String code) {
        if (code == null) return null;
        for (ProjectLevelEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
