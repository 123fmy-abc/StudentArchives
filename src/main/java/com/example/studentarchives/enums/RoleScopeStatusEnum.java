package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色组织范围状态枚举（对齐 role_scopes.status）
 * <p>
 * 学生档案系统表 role_scopes.status
 * 0=停用 1=启用 2=过期
 * <p>
 * 注意：此枚举与通用 StatusEnum 不同，通用 StatusEnum 仅含 0=禁用 1=启用，
 * role_scopes 额外有 2=过期 状态
 */
@Getter
@AllArgsConstructor
public enum RoleScopeStatusEnum {

    DISABLED(0, "停用"),
    ENABLED(1, "启用"),
    EXPIRED(2, "过期"),
    ;

    private final int value;
    private final String label;

    public static RoleScopeStatusEnum of(Integer value) {
        if (value == null) return DISABLED;
        for (RoleScopeStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return DISABLED;
    }

    /** 是否有效（启用状态为有效） */
    public boolean isValid() {
        return this == ENABLED;
    }
}
