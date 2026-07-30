package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举（对齐 menus.type）
 * <p>
 * 学生档案系统表 menus.type
 * 1=menu菜单 2=button按钮 3=api接口
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    MENU(1, "菜单"),
    BUTTON(2, "按钮"),
    API(3, "api接口"),
    ;

    private final int value;
    private final String label;

    public static MenuTypeEnum of(Integer value) {
        if (value == null) return null;
        for (MenuTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
