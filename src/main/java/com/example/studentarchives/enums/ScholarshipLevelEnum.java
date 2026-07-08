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

    private final String value;
    private final String label;

    public static ScholarshipLevelEnum of(String value) {
        if (value == null) return null;
        for (ScholarshipLevelEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
