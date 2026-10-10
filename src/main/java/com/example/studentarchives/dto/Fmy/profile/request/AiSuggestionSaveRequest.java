package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 建议入库请求 DTO（POST /profile/career-plans/ai-suggestions）
 * <p>
 * 前端本地生成 AI 建议后调本端点落库拿回 aiSuggestionId，再调 4.15 ai-add 一键加入计划。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionSaveRequest {

    /** AI 建议内容 */
    @NotBlank(message = "建议内容不能为空")
    private String suggestionContent;

    /** 目标学期 ID（可选，仅校验存在，不落库） */
    private Long semesterId;

    /** 归属短板分析 ID（可选，用于归属校验） */
    private Long weaknessId;
}
