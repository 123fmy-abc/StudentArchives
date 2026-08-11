package com.example.studentarchives.dto.Fmy.indicator.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新指标请求 DTO（PUT /admin/indicators/{indicatorId}，文档 1.3）
 * <p>
 * 更新指标基础信息。若指标已关联生效中的规则版本，修改 weight/scoringRule 时
 * 系统在下次发布时自动创建新的规则版本快照，不影响历史评分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorUpdateRequest {

    /** 指标名称 */
    @Size(max = 100, message = "indicatorName 长度不能超过100")
    private String indicatorName;

    /** 权重（0-1） */
    @DecimalMin(value = "0", message = "weight 不能小于 0")
    @DecimalMax(value = "1", message = "weight 不能大于 1")
    private BigDecimal weight;

    /** 指标说明 */
    @Size(max = 500, message = "description 长度不能超过500")
    private String description;

    /** 计分规则 JSON，仅三级指标可修改 */
    private JsonNode scoringRule;

    /** 能力维度编码 */
    @Size(max = 50, message = "dimensionCode 长度不能超过50")
    private String dimensionCode;

    /** 排序 */
    private Integer sort;

    /** 0=禁用 1=启用 */
    private Integer status;
}
