package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI建议一键添加为计划响应 DTO（POST /profile/career-plans/ai-add）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiPlanCreateResponse {

    private Long planId;

    /** 固定为草稿(0) */
    private Integer status;

    private String statusLabel;

    /** 固定为 2（AI建议添加） */
    private Integer source;

    private String sourceLabel;

    private Integer requireConfirm;
}
