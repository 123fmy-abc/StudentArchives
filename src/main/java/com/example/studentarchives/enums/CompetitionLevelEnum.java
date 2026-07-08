package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 竞赛级别枚举
 */
@Getter
@AllArgsConstructor
public enum CompetitionLevelEnum {

    NATIONAL("national", "国家级"),
    PROVINCIAL("provincial", "省部级"),
    SCHOOL("school", "校级"),
    COLLEGE("college", "院级"),
    ;

    private final String value;
    private final String label;

    public static CompetitionLevelEnum of(String value) {
        if (value == null) return null;
        for (CompetitionLevelEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
