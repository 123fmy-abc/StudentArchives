package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 意见模板分类枚举（对齐 opinion_templates.category）
 * <p>
 * 学生档案系统表 opinion_templates.category
 * 1=通过意见 2=退回原因
 */
@Getter
@AllArgsConstructor
public enum OpinionTemplateCategoryEnum {

    APPROVE(1, "通过意见"),
    REJECT(2, "退回原因"),
    ;

    private final int value;
    private final String label;

    public static OpinionTemplateCategoryEnum of(Integer value) {
        if (value == null) return null;
        for (OpinionTemplateCategoryEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
