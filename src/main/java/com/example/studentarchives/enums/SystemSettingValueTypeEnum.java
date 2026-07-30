package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统设置值类型枚举（对齐 system_settings.value_type）
 * <p>
 * 学生档案系统表 system_settings.value_type
 * string/int/float/json/boolean
 */
@Getter
@AllArgsConstructor
public enum SystemSettingValueTypeEnum {

    STRING("string", "字符串"),
    INT("int", "整数"),
    FLOAT("float", "浮点数"),
    JSON("json", "JSON"),
    BOOLEAN("boolean", "布尔"),
    ;

    private final String value;
    private final String label;

    public static SystemSettingValueTypeEnum of(String value) {
        if (value == null) return null;
        for (SystemSettingValueTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
