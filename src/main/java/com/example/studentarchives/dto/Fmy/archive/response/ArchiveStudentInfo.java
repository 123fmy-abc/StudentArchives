package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 档案学生基础信息 DTO（GET /admin/archives/{archiveId}，文档 15.2）
 * <p>
 * 学生基本信息与组织归属（班级/专业/学院/年级），供管理端档案详情页展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveStudentInfo {

    /** 学生用户 ID */
    private Long userId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 性别：0=未知 1=男 2=女 */
    private Integer gender;

    /** 性别中文标签 */
    private String genderLabel;

    /** 班级名称 */
    private String className;

    /** 专业名称 */
    private String majorName;

    /** 学院名称 */
    private String collegeName;

    /** 年级，如 2023级 */
    private String grade;
}
