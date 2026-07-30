package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息投递状态枚举（对齐 message_deliveries.status）
 * <p>
 * 学生档案系统表 message_deliveries.status
 * 0=待投递 1=已投递 2=失败 3=已读（push专用）
 */
@Getter
@AllArgsConstructor
public enum MessageDeliveryStatusEnum {

    PENDING(0, "待投递"),
    DELIVERED(1, "已投递"),
    FAILED(2, "失败"),
    READ(3, "已读"),
    ;

    private final int value;
    private final String label;

    public static MessageDeliveryStatusEnum of(Integer value) {
        if (value == null) return PENDING;
        for (MessageDeliveryStatusEnum e : values()) {
            if (e.value == value) return e;
        }
        return PENDING;
    }
}
