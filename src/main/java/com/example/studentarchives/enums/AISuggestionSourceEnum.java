package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 建议来源枚举（对齐 ai_suggestions.source 等）
 * <p>
 * 学生档案系统表 ai_suggestions.source / weakness_improvements.source / career_goals.source 等
 * 1=AI生成 2=教师建议
 */
@Getter
@AllArgsConstructor
public enum AISuggestionSourceEnum {

    AI(1, "AI生成"),
    TEACHER(2, "教师建议"),
    ;

    private final int value;
    private final String label;

    public static AISuggestionSourceEnum of(Integer value) {
        if (value == null) return AI;
        for (AISuggestionSourceEnum e : values()) {
            if (e.value == value) return e;
        }
        return AI;
    }
}
