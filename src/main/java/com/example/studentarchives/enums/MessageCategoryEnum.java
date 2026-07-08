package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息分类枚举
 */
@Getter
@AllArgsConstructor
public enum MessageCategoryEnum {

    SYSTEM_NOTICE("system_notice", "系统通知"),
    AUDIT_REMIND("audit_remind", "审批提醒"),
    DYNAMIC_REMIND("dynamic_remind", "动态提醒"),
    PRIVATE_MESSAGE("private_message", "私信"),
    ;

    private final String value;
    private final String label;

    public static MessageCategoryEnum of(String value) {
        if (value == null) return null;
        for (MessageCategoryEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
