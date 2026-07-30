package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 组织架构类型枚举（对齐 statistics_configs.org_type）
 * <p>
 * 学生档案系统表 statistics_configs.org_type
 * 1=学校 2=学院 3=专业 4=班级
 */
@Getter
@AllArgsConstructor
public enum OrgTypeEnum {

    SCHOOL(1, "学校"),
    COLLEGE(2, "学院"),
    MAJOR(3, "专业"),
    CLASS(4, "班级"),
    ;

    private final int value;
    private final String label;

    public static OrgTypeEnum of(Integer value) {
        if (value == null) return null;
        for (OrgTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
