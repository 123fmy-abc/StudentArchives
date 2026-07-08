package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成长时间轴事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum EventTypeEnum {

    AWARD(1, "奖项"),
    GRADE(2, "成绩"),
    PRACTICE(3, "实践"),
    CAREER_PLAN(4, "职业规划"),
    WEAKNESS_IMPROVE(5, "短板改进"),
    ABILITY_UPGRADE(6, "能力提升"),
    ;

    private final int value;
    private final String label;

    public static EventTypeEnum of(Integer value) {
        if (value == null) return null;
        for (EventTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
