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

    private final String code;
    private final String name;

    public static CompetitionLevelEnum of(String code) {
        if (code == null) return null;
        for (CompetitionLevelEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
