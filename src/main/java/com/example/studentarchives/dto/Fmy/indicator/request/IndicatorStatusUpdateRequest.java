package com.example.studentarchives.dto.Fmy.indicator.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改指标状态请求 DTO（PATCH /admin/indicators/{indicatorId}/status，文档 1.8）
 * <p>
 * 仅变更某指标的启用/禁用状态，与通用更新接口（1.3）解耦。
 * 禁用时自动级联禁用其所有后代节点；启用时校验启用同级权重之和不超过父权重（见服务层）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorStatusUpdateRequest {

    /** 0=禁用 1=启用 */
    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 取值只能为 0=禁用 或 1=启用")
    @Max(value = 1, message = "status 取值只能为 0=禁用 或 1=启用")
    private Integer status;
}
