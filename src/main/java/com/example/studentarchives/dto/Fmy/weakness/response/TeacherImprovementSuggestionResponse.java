package com.example.studentarchives.dto.Fmy.weakness.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增改进建议响应 DTO（POST /teacher/students/{studentId}/improvement-suggestions，《教师端接口文档》10.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherImprovementSuggestionResponse {

    /** 建议 ID（improvement_suggestions.id） */
    private Long suggestionId;

    /** 建议创建时间（ISO 8601 带时区） */
    private String createdAt;
}
