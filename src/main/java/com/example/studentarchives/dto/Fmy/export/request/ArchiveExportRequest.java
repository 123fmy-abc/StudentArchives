package com.example.studentarchives.dto.Fmy.export.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 一键导出学生档案（管理端）提交请求
 * <p>对应管理端接口文档 5.11 POST /admin/exports/archives</p>
 */
@Getter
@Setter
public class ArchiveExportRequest {

    /** 学期ID；为空时取当前学期 */
    private Long semesterId;

    /** 范围类型：1学校 2学院 3专业 4班级 6年级，必填 */
    @NotNull(message = "范围类型不能为空")
    private Integer scopeType;

    /** 范围ID：学院ID/专业ID/班级ID；范围类型为学校或年级时可为空 */
    private Long scopeId;

    /** 年级（范围类型为年级时使用，例如 2024） */
    private String grade;

    /** 文件类型：pdf/xlsx，必填 */
    @NotBlank(message = "文件类型不能为空")
    private String fileType;

    /** 导出模板ID；为空时使用学校默认学生档案模板 */
    private Long templateId;

    /** 档案组成部分：basic_info/academicInfo/dimensionScores/awards/practices/careerPlans；为空时导出全部 */
    private List<String> sections;

    /** 档案状态筛选：1在读 2休学 3毕业 4退学；为空时导出全部 */
    private Integer archiveStatus;

    /** 用途：internal 内部（默认，PDF 加水印）/ external 外部（不加水印） */
    private String purpose;

    /** 是否包含数据版本与字段说明信息，默认 true */
    private Boolean includeMetadata;
}
