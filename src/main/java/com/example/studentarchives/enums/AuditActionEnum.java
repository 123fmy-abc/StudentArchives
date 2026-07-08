package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批动作枚举
 */
@Getter
@AllArgsConstructor
public enum AuditActionEnum {

    APPROVE(1, "通过"),
    REJECT(2, "驳回"),
    TRANSFER(3, "转交"),
    SKIP(4, "跳过"),
    ;

    private final int value;
    private final String label;

    public static AuditActionEnum of(Integer value) {
        if (value == null) return null;
        for (AuditActionEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
