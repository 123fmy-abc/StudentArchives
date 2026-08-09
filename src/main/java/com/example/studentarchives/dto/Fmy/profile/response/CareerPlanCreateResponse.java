package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增/提交职业规划响应 DTO（POST /profile/career-plans）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanCreateResponse {

    private Long planId;

    /** 0=草稿 1=待审批 */
    private Integer status;

    private String statusLabel;

    private Integer currentVersion;

    private Integer submitCount;
}
