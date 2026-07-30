package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色类型枚举（对齐 roles.role_type）
 * <p>
 * 学生档案系统表 roles.role_type
 * 1=教学类 2=行政类 3=审核类 4=系统管理类
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum {

    TEACHING(1, "教学类"),
    ADMINISTRATIVE(2, "行政类"),
    AUDIT(3, "审核类"),
    SYSTEM_MANAGEMENT(4, "系统管理类"),
    ;

    private final int value;
    private final String label;

    public static RoleTypeEnum of(Integer value) {
        if (value == null) return null;
        for (RoleTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
