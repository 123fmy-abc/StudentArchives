package com.example.studentarchives.dto.Fmy.career.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交职业规划反馈请求 DTO（POST /teacher/career-plans/{planId}/feedbacks，《教师端接口文档》8.1）
 * <p>
 * feedbackContent 必填；suggestionItems 为结构化建议项列表，存入
 * career_plan_feedbacks.suggestion_items（JSON 数组）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCareerFeedbackRequest {

    /** 反馈内容（必填） */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容长度不能超过2000")
    private String feedbackContent;

    /** 建议项列表（可选），存入 suggestion_items JSON 数组 */
    private List<String> suggestionItems;
}
