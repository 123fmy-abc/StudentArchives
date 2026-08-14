package com.example.studentarchives.dto.Fmy.indicator.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量修改指标状态请求 DTO（PATCH /admin/indicators/status，文档 1.9）
 * <p>
 * 对一批指标统一启用/禁用。整体一个事务，任一指标校验失败（如启用后同级权重超过父权重、
 * 指标不存在、跨学校混合）则整批不生效（fail-fast）。
 * 禁用时对列表内每个指标级联禁用其所有后代节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorStatusBatchRequest {

    /** 待修改状态的指标 ID 列表（1~100 个） */
    @NotEmpty(message = "indicatorIds 不能为空")
    @Size(min = 1, max = 100, message = "indicatorIds 数量需在 1~100 之间")
    private List<Long> indicatorIds;

    /** 0=禁用 1=启用 */
    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 取值只能为 0=禁用 或 1=启用")
    @Max(value = 1, message = "status 取值只能为 0=禁用 或 1=启用")
    private Integer status;
}
