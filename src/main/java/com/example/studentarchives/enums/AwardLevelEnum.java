package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 获奖等级枚举
 */
@Getter
@AllArgsConstructor
public enum AwardLevelEnum {

    SPECIAL("special", "特等奖"),
    FIRST("first", "一等奖"),
    SECOND("second", "二等奖"),
    THIRD("third", "三等奖"),
    EXCELLENCE("excellence", "优秀奖"),
    ;

    private final String code;
    private final String name;

    public static AwardLevelEnum of(String code) {
        if (code == null) return null;
        for (AwardLevelEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
