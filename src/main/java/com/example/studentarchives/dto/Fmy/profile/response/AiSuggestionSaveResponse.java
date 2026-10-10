package com.example.studentarchives.dto.Fmy.profile.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 建议入库响应 DTO（POST /profile/career-plans/ai-suggestions）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionSaveResponse {

    /** AI 建议 ID（improvement_suggestions.id） */
    private Long aiSuggestionId;
}
