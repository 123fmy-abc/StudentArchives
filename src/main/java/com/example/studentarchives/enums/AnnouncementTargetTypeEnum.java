package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公告发布对象枚举（对齐 announcements.target_type）
 * <p>
 * 学生档案系统表 announcements.target_type
 * all/college/major/class
 */
@Getter
@AllArgsConstructor
public enum AnnouncementTargetTypeEnum {

    ALL("all", "全部"),
    COLLEGE("college", "学院"),
    MAJOR("major", "专业"),
    CLASS("class", "班级"),
    ;

    private final String value;
    private final String label;

    public static AnnouncementTargetTypeEnum of(String value) {
        if (value == null) return null;
        for (AnnouncementTargetTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
