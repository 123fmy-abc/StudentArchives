package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价指标层级枚举（对齐 evaluation_indicators.level）
 * <p>
 * 学生档案系统表 evaluation_indicators.level
 * 1/2/3
 */
@Getter
@AllArgsConstructor
public enum EvaluationIndicatorLevelEnum {

    LEVEL_1(1, "一级指标"),
    LEVEL_2(2, "二级指标"),
    LEVEL_3(3, "三级指标"),
    ;

    private final int value;
    private final String label;

    public static EvaluationIndicatorLevelEnum of(Integer value) {
        if (value == null) return null;
        for (EvaluationIndicatorLevelEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
