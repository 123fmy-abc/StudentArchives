package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师端学生短板分析项响应 DTO（GET /teacher/students/{studentId}/weaknesses，文档 12.6.1）
 * <p>
 * 数据来源：{@code weakness_analyses}，与学生端 4.1 个人档案短板分析同源；
 * {@code source} 对应 {@code weakness_analyses.source}（1=AI生成 2=教师建议），
 * {@code sourceLabel} 由 {@link com.example.studentarchives.enums.AISuggestionSourceEnum} 补充。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherWeaknessItemResponse {

    /** 短板分析 ID */
    private Long id;

    /** 短板类型 */
    private String weaknessType;

    /** 短板描述 */
    private String weaknessDesc;

    /** 严重程度 1-5 */
    private Integer severityLevel;

    /** 来源：1=AI生成 2=教师建议 */
    private Integer source;

    /** 来源展示名称 */
    private String sourceLabel;

    /** 0=未读 1=已读 */
    private Integer isRead;

    /** 创建时间（ISO 8601 带时区） */
    private String createdAt;
}
