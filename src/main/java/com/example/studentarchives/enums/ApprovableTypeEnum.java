package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批流程适用类型枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovableTypeEnum {

    ARCHIVE("Archive", "档案"),
    AWARD_APPLICATION("AwardApplication", "奖项报名"),
    CAREER_PLAN("CareerPlan", "职业规划"),
    ;

    private final String code;
    private final String name;

    public static ApprovableTypeEnum of(String code) {
        if (code == null) return null;
        for (ApprovableTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
