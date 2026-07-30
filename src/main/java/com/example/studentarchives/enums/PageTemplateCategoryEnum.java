package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 页面模板分类枚举（对齐 page_templates.category）
 * <p>
 * 学生档案系统表 page_templates.category
 * archive/award/career_plan
 */
@Getter
@AllArgsConstructor
public enum PageTemplateCategoryEnum {

    ARCHIVE("archive", "档案"),
    AWARD("award", "奖项"),
    CAREER_PLAN("career_plan", "职业规划"),
    ;

    private final String value;
    private final String label;

    public static PageTemplateCategoryEnum of(String value) {
        if (value == null) return null;
        for (PageTemplateCategoryEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
