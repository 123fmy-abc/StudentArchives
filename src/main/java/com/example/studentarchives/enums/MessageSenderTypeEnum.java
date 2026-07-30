package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息发送者类型枚举（对齐 messages.sender_type）
 * <p>
 * 学生档案系统表 messages.sender_type
 * 1=系统 2=人工 3=自动触发
 */
@Getter
@AllArgsConstructor
public enum MessageSenderTypeEnum {

    SYSTEM(1, "系统"),
    MANUAL(2, "人工"),
    AUTO(3, "自动触发"),
    ;

    private final int value;
    private final String label;

    public static MessageSenderTypeEnum of(Integer value) {
        if (value == null) return null;
        for (MessageSenderTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
