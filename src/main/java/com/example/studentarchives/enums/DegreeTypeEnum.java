package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学历层次枚举（对应 majors.degree_type）
 */
@Getter
@AllArgsConstructor
public enum DegreeTypeEnum {

    ASSOCIATE("associate", "专科"),
    UNDERGRADUATE("undergraduate", "本科"),
    MASTER("master", "研究生/硕士"),
    DOCTOR("doctor", "博士"),
    POSTDOCTOR("postdoctor", "博士后"),
    ;

    private final String value;
    private final String label;

    public static DegreeTypeEnum of(String value) {
        if (value == null) return null;
        for (DegreeTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
