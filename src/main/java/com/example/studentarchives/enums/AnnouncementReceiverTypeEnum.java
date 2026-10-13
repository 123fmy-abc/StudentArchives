package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公告接收对象枚举（对齐 announcements.receiver_type）
 * <p>
 * 与 announcements.target_type（接收范围：all/college/major/class）正交：
 * receiver_type 决定「发给谁」，target_type 决定「哪个组织范围」。
 * <ul>
 *   <li>all：全员（学生 + 教职工 + 管理员）</li>
 *   <li>student：学生</li>
 *   <li>staff：教职工（教师类 level 2~6，不含管理员）</li>
 *   <li>admin：管理员（level 0，全校范围）</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum AnnouncementReceiverTypeEnum {

    ALL("all", "全员"),
    STUDENT("student", "学生"),
    STAFF("staff", "教职工"),
    ADMIN("admin", "管理员"),
    ;

    private final String value;
    private final String label;

    public static AnnouncementReceiverTypeEnum of(String value) {
        if (value == null) return null;
        for (AnnouncementReceiverTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}