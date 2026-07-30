package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 字段权限范围关系枚举（对齐 field_permissions.scope_relation）
 * <p>
 * 学生档案系统表 field_permissions.scope_relation
 * AND=叠加 OR=扩大 OVERRIDE=覆盖
 */
@Getter
@AllArgsConstructor
public enum FieldPermissionScopeRelationEnum {

    AND("AND", "叠加"),
    OR("OR", "扩大"),
    OVERRIDE("OVERRIDE", "覆盖"),
    ;

    private final String value;
    private final String label;

    public static FieldPermissionScopeRelationEnum of(String value) {
        if (value == null) return null;
        for (FieldPermissionScopeRelationEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
