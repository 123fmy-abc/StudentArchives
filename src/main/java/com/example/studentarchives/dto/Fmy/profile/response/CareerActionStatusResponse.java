package com.example.studentarchives.dto.Fmy.profile.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新行动状态响应 DTO（PUT /profile/career-plans/{planId}/actions/{actionId}/status）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerActionStatusResponse {

    private Long actionId;

    /** 0=未开始 1=进行中 2=已完成 */
    private Integer status;

    private String statusLabel;

    private Integer completionRate;
}
