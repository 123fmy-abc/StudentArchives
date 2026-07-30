package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批待办状态枚举（对齐 approval_todos.status）
 * <p>
 * 学生档案系统表 approval_todos.status
 * 1=待审批 2=审批中 3=已委托
 */
@Getter
@AllArgsConstructor
public enum ApprovalTodoStatusEnum {

    PENDING(1, "待审批"),
    IN_PROGRESS(2, "审批中"),
    DELEGATED(3, "已委托"),
    ;

    private final int value;
    private final String label;

    public static ApprovalTodoStatusEnum of(Integer value) {
        if (value == null) return null;
        for (ApprovalTodoStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
