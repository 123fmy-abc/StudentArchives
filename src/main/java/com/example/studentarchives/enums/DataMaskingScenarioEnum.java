package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据脱敏适用场景枚举（对齐 data_masking_rules.apply_scenarios）
 * <p>
 * 学生档案系统表 data_masking_rules.apply_scenarios
 * export/api/screen/log
 */
@Getter
@AllArgsConstructor
public enum DataMaskingScenarioEnum {

    EXPORT("export", "导出"),
    API("api", "接口"),
    SCREEN("screen", "屏幕"),
    LOG("log", "日志"),
    ;

    private final String value;
    private final String label;

    public static DataMaskingScenarioEnum of(String value) {
        if (value == null) return null;
        for (DataMaskingScenarioEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
