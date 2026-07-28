package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 附件状态枚举（对齐 attachment_relations.file_status）
 * <p>
 * 学生档案系统表 attachment_relations.file_status
 * 1=暂存（未绑定） 2=已绑定 3=已删除（软删标记）
 */
@Getter
@AllArgsConstructor
public enum FileStatusEnum {

    TEMP(1, "暂存"),
    BOUND(2, "已绑定"),
    DELETED(3, "已删除"),
    ;

    private final int value;
    private final String label;

    public static FileStatusEnum of(Integer value) {
        if (value == null) return null;
        for (FileStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }

    /** 是否可绑定业务（仅暂存状态可绑定） */
    public boolean isBindable() {
        return this == TEMP;
    }
}
