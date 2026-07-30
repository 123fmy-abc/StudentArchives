package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 附件预览转换状态枚举（对齐 attachment_relations.convert_status）
 * <p>
 * 学生档案系统表 attachment_relations.convert_status
 * 0=待处理 1=已生成预览 2=失败
 */
@Getter
@AllArgsConstructor
public enum ConvertStatusEnum {

    PENDING(0, "待处理"),
    DONE(1, "已生成预览"),
    FAILED(2, "失败"),
    ;

    private final int value;
    private final String label;

    public static ConvertStatusEnum of(Integer value) {
        if (value == null) return PENDING;
        for (ConvertStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return PENDING;
    }
}
