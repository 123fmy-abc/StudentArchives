package com.example.studentarchives.dto.Fmy.weakness.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增改进建议请求 DTO（POST /teacher/students/{studentId}/improvement-suggestions，《教师端接口文档》10.1）
 * <p>
 * suggestionType 与 content 必填；weaknessId / relatedGoalId 可选。
 * source 恒为 2=教师建议，teacher_id 为当前登录教师。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherImprovementSuggestionRequest {

    /** 关联短板 ID（可选），对应 improvement_suggestions.weakness_id */
    private Long weaknessId;

    /** 建议类型（必填），对应 improvement_suggestions.suggestion_type，如 research / practice */
    @NotBlank(message = "建议类型不能为空")
    @Size(max = 50, message = "建议类型长度不能超过50")
    private String suggestionType;

    /** 建议内容（必填），对应 improvement_suggestions.suggestion_content */
    @NotBlank(message = "建议内容不能为空")
    @Size(max = 2000, message = "建议内容长度不能超过2000")
    private String content;

    /** 关联目标 ID（可选），对应 improvement_suggestions.related_goal_id */
    private Long relatedGoalId;
}
