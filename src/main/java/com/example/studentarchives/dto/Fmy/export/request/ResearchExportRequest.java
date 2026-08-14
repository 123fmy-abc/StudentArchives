package com.example.studentarchives.dto.Fmy.export.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 研究数据导出请求 DTO（POST /admin/exports/research，管理端文档 5.1）
 * <p>
 * 导出用于研究分析的学生数据，默认使用匿名编号替代姓名和学号。
 * 仅拥有 {@code export:research} 权限的用户可调用。
 */
@Data
public class ResearchExportRequest {

    /** 学期 ID（必填） */
    @NotNull(message = "semesterId 不能为空")
    private Long semesterId;

    /** 范围类型：1=学校 2=学院 3=专业 4=班级 6=年级（必填） */
    @NotNull(message = "scopeType 不能为空")
    private Integer scopeType;

    /** 范围 ID：scopeType=1(学校) 或 scopeType=6(年级) 时无需传 */
    private Long scopeId;

    /** 年级筛选，如 2023级：scopeType=6(年级) 时必填 */
    private String grade;

    /** 导出数据类型：archives/scores/audits/ai/career（必填，非空） */
    @NotEmpty(message = "dataTypes 不能为空")
    private List<String> dataTypes;

    /** 各数据类型可选字段映射（仅入审计快照，本次导出输出各类型标准字段） */
    private Map<String, Object> fields;

    /** 是否匿名化，默认 true（用匿名编号替代姓名和学号） */
    private Boolean isAnonymized = true;

    /** 是否包含字段说明和数据版本，默认 true */
    private Boolean includeMetadata = true;
}
