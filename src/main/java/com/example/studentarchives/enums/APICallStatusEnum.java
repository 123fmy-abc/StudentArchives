package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 调用状态枚举（对齐 api_call_logs.call_status）
 * <p>
 * 学生档案系统表 api_call_logs.call_status
 * 0=失败 1=成功 2=重试
 */
@Getter
@AllArgsConstructor
public enum APICallStatusEnum {

    FAILED(0, "失败"),
    SUCCESS(1, "成功"),
    RETRY(2, "重试"),
    ;

    private final int value;
    private final String label;

    public static APICallStatusEnum of(Integer value) {
        if (value == null) return FAILED;
        for (APICallStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return FAILED;
    }
}
