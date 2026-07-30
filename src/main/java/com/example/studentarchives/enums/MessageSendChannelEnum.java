package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息发送渠道枚举（对齐 messages.send_channel / message_deliveries.channel）
 * <p>
 * 学生档案系统表 messages.send_channel
 * push/email/sms
 */
@Getter
@AllArgsConstructor
public enum MessageSendChannelEnum {

    PUSH("push", "站内推送"),
    EMAIL("email", "邮件"),
    SMS("sms", "短信"),
    ;

    private final String value;
    private final String label;

    public static MessageSendChannelEnum of(String value) {
        if (value == null) return null;
        for (MessageSendChannelEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
