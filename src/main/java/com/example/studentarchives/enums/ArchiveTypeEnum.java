package com.example.studentarchives.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 档案类型枚举（对应 archive_type_config 的 archive_type 编码）
 */
@Getter
@AllArgsConstructor
public enum ArchiveTypeEnum {

    ACADEMIC_COMPETITION("academic_competition", "学科竞赛"),
    INNOVATION_ENTREPRENEURSHIP("innovation_entrepreneurship", "创新创业"),
    ACADEMIC_RESEARCH("academic_research", "学术研究"),
    SCHOLARSHIP("scholarship", "奖学金"),
    HONOR_CERTIFICATE("honor_certificate", "荣誉证书"),
    INTERNSHIP("internship", "实习经历"),
    ORGANIZATION("organization", "组织履历"),
    TRAINING_PROJECT("training_project", "实训项目"),
    SOCIAL_PRACTICE("social_practice", "社会实践"),
    BOOK_REVIEW("book_review", "图书心得"),
    ;

    private final String value;
    private final String label;

    public static ArchiveTypeEnum of(String value) {
        if (value == null) return null;
        for (ArchiveTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
