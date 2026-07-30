package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 附件业务类型枚举（对齐 attachment_relations.biz_type）
 * <p>
 * 学生档案系统表 attachment_relations.biz_type
 * archive/award/career_plan/announcement 等
 */
@Getter
@AllArgsConstructor
public enum AttachmentBizTypeEnum {

    ARCHIVE("archive", "档案"),
    AWARD("award", "奖项"),
    CAREER_PLAN("career_plan", "职业规划"),
    ANNOUNCEMENT("announcement", "公告"),
    ;

    private final String value;
    private final String label;

    public static AttachmentBizTypeEnum of(String value) {
        if (value == null) return null;
        for (AttachmentBizTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
