package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 反馈动作枚举（对齐 ai_feedback_records.action）
 * <p>
 * 学生档案系统表 ai_feedback_records.action
 * 1=采纳 2=修改后采纳 3=拒绝
 */
@Getter
@AllArgsConstructor
public enum AIFeedbackActionEnum {

    ADOPT(1, "采纳"),
    ADOPT_WITH_MODIFICATION(2, "修改后采纳"),
    REJECT(3, "拒绝"),
    ;

    private final int value;
    private final String label;

    public static AIFeedbackActionEnum of(Integer value) {
        if (value == null) return null;
        for (AIFeedbackActionEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
