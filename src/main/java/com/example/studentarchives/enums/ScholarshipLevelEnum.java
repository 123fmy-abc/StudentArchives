package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 奖学金类别枚举
 */
@Getter
@AllArgsConstructor
public enum ScholarshipLevelEnum {

    NATIONAL("national", "国家奖学金"),
    PROVINCIAL("provincial", "省级奖学金"),
    SCHOOL("school", "校级奖学金"),
    ENTERPRISE("enterprise", "企业奖学金"),
    ;

    private final String code;
    private final String name;

    public static ScholarshipLevelEnum of(String code) {
        if (code == null) return null;
        for (ScholarshipLevelEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
