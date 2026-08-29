package com.example.studentarchives.dto.Fmy.career.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交职业规划反馈响应 DTO（POST /teacher/career-plans/{planId}/feedbacks，《教师端接口文档》8.1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherCareerFeedbackResponse {

    /** 反馈 ID（career_plan_feedbacks.id） */
    private Long feedbackId;

    /** 反馈提交时间（ISO 8601 带时区） */
    private String createdAt;
}
