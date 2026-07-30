package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作日志关联业务类型枚举（对齐 operation_logs.related_type）
 * <p>
 * 学生档案系统表 operation_logs.related_type
 * archive/award_application/career_plan/growth_timeline
 */
@Getter
@AllArgsConstructor
public enum OperationLogRelatedTypeEnum {

    ARCHIVE("archive", "档案"),
    AWARD_APPLICATION("award_application", "奖项报名"),
    CAREER_PLAN("career_plan", "职业规划"),
    GROWTH_TIMELINE("growth_timeline", "成长时间轴"),
    ;

    private final String value;
    private final String label;

    public static OperationLogRelatedTypeEnum of(String value) {
        if (value == null) return null;
        for (OperationLogRelatedTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
