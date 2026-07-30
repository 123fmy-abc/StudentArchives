package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批动作枚举（对齐 audit_logs.action）
 * <p>
 * 学生档案系统表 audit_logs.action
 * 1=通过 2=退回 3=撤回 4=转交
 */
@Getter
@AllArgsConstructor
public enum AuditActionEnum {

    APPROVE(1, "通过"),
    REJECT(2, "退回"),
    WITHDRAW(3, "撤回"),
    TRANSFER(4, "转交"),
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
