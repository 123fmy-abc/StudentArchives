package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批节点动作枚举（对齐 approval_nodes.action）
 * <p>
 * 学生档案系统表 附录G2：approval_nodes.action
 * NULL=待审核 1=通过 2=退回 3=转交 4=跳过
 * <p>
 * 注意：audit_logs.action 的枚举值不同（1=通过 2=退回 3=撤回 4=转交），
 * 请使用 AuditActionEnum
 */
@Getter
@AllArgsConstructor
public enum ApprovalNodeActionEnum {

    PENDING(null, "待审核"),
    APPROVE(1, "通过"),
    REJECT(2, "退回"),
    TRANSFER(3, "转交"),
    SKIP(4, "跳过"),
    ;

    private final Integer value;
    private final String label;

    public static ApprovalNodeActionEnum of(Integer value) {
        if (value == null) return PENDING;
        for (ApprovalNodeActionEnum e : values()) {
            if (e.value != null && e.value.equals(value)) return e;
        }
        return PENDING;
    }
}
