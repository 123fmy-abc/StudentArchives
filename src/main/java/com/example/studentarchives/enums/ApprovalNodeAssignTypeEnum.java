package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批节点分配方式枚举（对齐 approval_nodes.assign_type）
 * <p>
 * 学生档案系统表 approval_nodes.assign_type
 * 1=自动分配 2=手动指定 3=系统指定 4=上级指定
 */
@Getter
@AllArgsConstructor
public enum ApprovalNodeAssignTypeEnum {

    AUTO(1, "自动分配"),
    MANUAL(2, "手动指定"),
    SYSTEM(3, "系统指定"),
    SUPERIOR(4, "上级指定"),
    ;

    private final int value;
    private final String label;

    public static ApprovalNodeAssignTypeEnum of(Integer value) {
        if (value == null) return null;
        for (ApprovalNodeAssignTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
