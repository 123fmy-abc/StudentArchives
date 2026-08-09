package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复制上一学期计划响应 DTO（POST /profile/career-plans/copy）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerPlanCopyResponse {

    private Long planId;

    private Long copyFromId;

    /** 复制后状态固定为草稿(0) */
    private Integer status;

    private String statusLabel;
}
