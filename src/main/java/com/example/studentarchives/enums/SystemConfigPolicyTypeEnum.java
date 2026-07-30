package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统配置策略类型枚举（对齐 system_configs.policy_type）
 * <p>
 * 学生档案系统表 system_configs.policy_type
 * password/session/backup/retention/login_lock/masking_default
 */
@Getter
@AllArgsConstructor
public enum SystemConfigPolicyTypeEnum {

    PASSWORD("password", "密码策略"),
    SESSION("session", "会话策略"),
    BACKUP("backup", "备份策略"),
    RETENTION("retention", "保留策略"),
    LOGIN_LOCK("login_lock", "登录锁定策略"),
    MASKING_DEFAULT("masking_default", "默认脱敏策略"),
    ;

    private final String value;
    private final String label;

    public static SystemConfigPolicyTypeEnum of(String value) {
        if (value == null) return null;
        for (SystemConfigPolicyTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
