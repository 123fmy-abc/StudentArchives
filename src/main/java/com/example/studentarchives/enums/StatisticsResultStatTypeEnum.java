package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统计结果统计类型枚举（对齐 statistics_results.stat_type）
 * <p>
 * 学生档案系统表 statistics_results.stat_type
 * archive/award/gpa 等
 */
@Getter
@AllArgsConstructor
public enum StatisticsResultStatTypeEnum {

    ARCHIVE("archive", "档案"),
    AWARD("award", "奖项"),
    GPA("gpa", "成绩"),
    ;

    private final String value;
    private final String label;

    public static StatisticsResultStatTypeEnum of(String value) {
        if (value == null) return null;
        for (StatisticsResultStatTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
