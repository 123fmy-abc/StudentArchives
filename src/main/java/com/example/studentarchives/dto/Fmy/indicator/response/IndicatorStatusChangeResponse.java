package com.example.studentarchives.dto.Fmy.indicator.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改指标状态响应 DTO（文档 1.8 / 1.9）
 * <p>
 * 单条接口返回 {@code indicatorId}，批量接口不返回；两者均返回变更后的目标状态与实际影响数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorStatusChangeResponse {

    /** 目标指标 ID（批量接口不返回，null 时省略该字段） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long indicatorId;

    /** 变更后的目标状态：0=禁用 1=启用 */
    private Integer status;

    /** 实际变更状态的指标数量（含禁用时级联禁用的后代） */
    private Integer affectedCount;

    /** 禁用时级联禁用的后代数量（启用为 0） */
    private Integer descendantCount;
}
