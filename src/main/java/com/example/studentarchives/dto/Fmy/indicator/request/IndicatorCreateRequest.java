package com.example.studentarchives.dto.Fmy.indicator.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建指标请求 DTO（POST /admin/indicators，文档 1.2）
 * <p>
 * 创建一级/二级/三级指标。创建三级指标时必须填写 scoringRule。
 * 权重取值 0-1，同级指标权重之和应为父级权重（一级指标之和为 1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorCreateRequest {

    /** 学校 ID */
    @NotNull(message = "schoolId 不能为空")
    private Long schoolId;

    /** 父级指标 ID，为空则创建一级指标 */
    private Long parentId;

    /** 指标编码，学校内全局唯一 */
    @NotBlank(message = "indicatorCode 不能为空")
    @Size(max = 50, message = "indicatorCode 长度不能超过50")
    private String indicatorCode;

    /** 指标名称 */
    @NotBlank(message = "indicatorName 不能为空")
    @Size(max = 100, message = "indicatorName 长度不能超过100")
    private String indicatorName;

    /** 权重（0-1） */
    @NotNull(message = "weight 不能为空")
    @DecimalMin(value = "0", message = "weight 不能小于 0")
    @DecimalMax(value = "1", message = "weight 不能大于 1")
    private BigDecimal weight;

    /** 指标说明 */
    @Size(max = 500, message = "description 长度不能超过500")
    private String description;

    /** 计分规则 JSON，三级指标必填。type 取值：AVG/SUM/MAX/WEIGHTED/THRESHOLD/COUNT */
    private JsonNode scoringRule;

    /** 能力维度编码（一级指标可选；二级/三级指标自动继承父级） */
    @Size(max = 50, message = "dimensionCode 长度不能超过50")
    private String dimensionCode;

    /** 排序，默认取同级末位 +1 */
    private Integer sort;
}
