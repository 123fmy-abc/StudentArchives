package com.example.studentarchives.enums;

/**
 * 敏感数据类型枚举
 * <p>
 * 用于 {@link com.example.studentarchives.annotation.Sensitive} 注解，
 * 指定序列化时的脱敏策略。
 */
public enum SensitiveType {

    /** 手机号：13812345678 → 138****5678 */
    PHONE,

    /** 邮箱：username@example.com → u***@example.com */
    EMAIL,
}
