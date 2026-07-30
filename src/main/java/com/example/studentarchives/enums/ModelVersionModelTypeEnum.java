package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 版本记录业务类型枚举（对齐 model_versions.model_type）
 * <p>
 * 学生档案系统表 model_versions.model_type
 * archive/award_application/career_plan/export_template 等
 */
@Getter
@AllArgsConstructor
public enum ModelVersionModelTypeEnum {

    ARCHIVE("archive", "档案"),
    AWARD_APPLICATION("award_application", "奖项报名"),
    CAREER_PLAN("career_plan", "职业规划"),
    EXPORT_TEMPLATE("export_template", "导出模板"),
    ;

    private final String value;
    private final String label;

    public static ModelVersionModelTypeEnum of(String value) {
        if (value == null) return null;
        for (ModelVersionModelTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
