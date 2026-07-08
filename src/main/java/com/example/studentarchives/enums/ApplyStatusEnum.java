package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 档案/奖项/职业规划 通用申报状态枚举
 * <p>
 * 0=草稿 1=待审批 2=通过 3=驳回 4=需修改
 */
@Getter
@AllArgsConstructor
public enum ApplyStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "待审批"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    NEED_MODIFY(4, "需修改"),
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

    /** 是否允许编辑（草稿/驳回/需修改 状态可编辑） */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED || this == NEED_MODIFY;
    }

    /** 是否终态（已通过/已驳回 为终态） */
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
