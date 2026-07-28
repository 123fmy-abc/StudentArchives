package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批流程实例状态枚举（对齐 approval_instances.status）
 * <p>
 * 学生档案系统表 approval_instances.status
 * 1=审批中 2=已通过 3=已退回 4=已撤回
 */
@Getter
@AllArgsConstructor
public enum ApprovalInstanceStatusEnum {

    PENDING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已退回"),
    REVOKED(4, "已撤回"),
    ;

    private final int value;
    private final String label;

    public static ApprovalInstanceStatusEnum of(Integer value) {
        if (value == null) return null;
        for (ApprovalInstanceStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }

    /** 是否终态 */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == REVOKED;
    }
}
