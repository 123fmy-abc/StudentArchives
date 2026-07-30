package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 档案/奖项/职业规划 通用申报状态枚举
 * <p>
 * 对齐学生档案系统表：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销
 */
@Getter
@AllArgsConstructor
public enum ApplyStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "待审批"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已退回"),
    REVOKED(4, "已撤销"),
    ;

    private final int value;
    private final String label;

    public static ApplyStatusEnum of(Integer value) {
        if (value == null) return DRAFT;
        for (ApplyStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return DRAFT;
    }

    /** 是否允许编辑（草稿/已退回/已撤销 状态可编辑） */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED || this == REVOKED;
    }

    /** 是否终态（已通过/已退回/已撤销 为终态） */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == REVOKED;
    }
}
