package com.example.studentarchives.dto.Fmy.archive.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生档案列表项 DTO（GET /admin/archives，文档 15.1）
 * <p>
 * 档案摘要与学生基础信息、组织归属，支撑管理端「档案查看」列表页。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveAdminListItem {

    /** 档案 ID */
    private Long archiveId;

    /** 档案类型编码，如 competition / scholarship */
    private String archiveType;

    /** 档案类型名称 */
    private String archiveTypeName;

    /** 档案标题 */
    private String title;

    /** 学期 ID */
    private Long semesterId;

    /** 学期名称 */
    private String semesterName;

    /** 获得时间（yyyy-MM-dd） */
    private String obtainedAt;

    /** 档案状态：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销 */
    private Integer status;

    /** 状态中文标签 */
    private String statusLabel;

    /** 学生用户 ID */
    private Long userId;

    /** 学号 */
    private String studentNo;

    /** 学生姓名 */
    private String studentName;

    /** 班级名称 */
    private String className;

    /** 专业名称 */
    private String majorName;

    /** 学院名称 */
    private String collegeName;

    /** 年级，如 2023级 */
    private String grade;

    /** 提交时间（ISO 8601 带时区） */
    private String submittedAt;
}
