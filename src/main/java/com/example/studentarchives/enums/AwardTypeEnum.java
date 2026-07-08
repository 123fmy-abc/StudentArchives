package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 奖项类型枚举
 */
@Getter
@AllArgsConstructor
public enum AwardTypeEnum {

    COMPETITION_STAR("competition_star", "竞赛之星"),
    RESEARCH_STAR("research_star", "科研之星"),
    INNOVATION_STAR("innovation_star", "双创之星"),
    ;

    private final String value;
    private final String label;

    public static AwardTypeEnum of(String value) {
        if (value == null) return null;
        for (AwardTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
