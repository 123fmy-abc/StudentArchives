package com.example.studentarchives.dto.Fmy.profile.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新职业规划行动状态请求 DTO（PUT /profile/career-plans/{planId}/actions/{actionId}/status）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareerActionStatusRequest {

    /** 0=未开始 1=进行中 2=已完成 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0/1/2")
    @Max(value = 2, message = "状态只能为0/1/2")
    private Integer status;
}
