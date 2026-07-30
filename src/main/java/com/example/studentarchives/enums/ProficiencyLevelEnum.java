package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 熟练程度枚举（对齐 skill_tags.proficiency_level）
 * <p>
 * 学生档案系统表 skill_tags.proficiency_level
 * 1=入门 2=一般 3=熟练 4=精通
 */
@Getter
@AllArgsConstructor
public enum ProficiencyLevelEnum {

    BEGINNER(1, "入门"),
    GENERAL(2, "一般"),
    PROFICIENT(3, "熟练"),
    EXPERT(4, "精通"),
    ;

    private final int value;
    private final String label;

    public static ProficiencyLevelEnum of(Integer value) {
        if (value == null) return BEGINNER;
        for (ProficiencyLevelEnum e : values()) {
            if (e.value == value) return e;
        }
        return BEGINNER;
    }
}
