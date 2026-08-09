package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI建议一键添加为计划请求 DTO（POST /profile/career-plans/ai-add）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPlanCreateRequest {

    /** AI建议 ID（improvement_suggestions.id） */
    @NotNull(message = "AI建议ID不能为空")
    private Long aiSuggestionId;

    /** 目标学期 ID */
    @NotNull(message = "学期不能为空")
    private Long semesterId;

    /** 规划标题，不传则自动生成 */
    @Size(max = 255, message = "规划标题长度不能超过255")
    private String title;

    /** 0=直接添加 1=返回草稿待确认，默认1 */
    private Integer requireConfirm = 1;
}
